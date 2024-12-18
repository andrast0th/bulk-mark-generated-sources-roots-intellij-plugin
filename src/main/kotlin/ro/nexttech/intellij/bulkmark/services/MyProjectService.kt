package ro.nexttech.intellij.bulkmark.services

import com.google.common.base.Objects
import com.intellij.ide.SaveAndSyncHandler
import com.intellij.ide.projectView.actions.MarkRootActionBase
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.*
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.toArray
import org.apache.commons.lang.StringUtils
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import java.util.function.Consumer
import javax.swing.SwingUtilities


@Service(Service.Level.PROJECT)
class MyProjectService(private val project: Project) {

    fun markGeneratedSourcesRoots(
        matchPattern: String,
        writeToConsole: Consumer<String>,
        isGeneratedSourceRoot: Boolean,
        isVerboseLogging: Boolean
    ) {

        if (isVerboseLogging) {
            writeToConsole.accept("\uD83D\uDCA5 Verbose logging enabled!")
        }

        if (StringUtils.isEmpty(matchPattern)) {
            writeToConsole.accept("Empty mask, skipping...")
            return
        }

        val regex = matchPattern.toRegex()
        writeToConsole.accept("Searching dirs matching mask: '$regex'")

        val allModuleDirs = ProjectRootManager.getInstance(project).contentRoots
        val rootDirs = findRootDirectories(allModuleDirs.toList()).toArray(emptyArray())

        markDirs(rootDirs, regex, writeToConsole, isGeneratedSourceRoot, isVerboseLogging)

        writeToConsole.accept("Done.")
    }

    private fun findRootDirectories(directories: List<VirtualFile>): List<VirtualFile> {
        val roots = directories.toMutableList()
        for (dir1 in directories) {
            for (dir2 in directories) {
                if (dir1 != dir2 && dir2.url.startsWith(dir1.url)) {
                    roots.remove(dir1)
                    break
                }
            }
        }
        return roots
    }

    private fun markDirs(
        dirs: Array<VirtualFile>,
        matchPattern: Regex,
        writeToConsole: Consumer<String>,
        isGeneratedSourceRoot: Boolean,
        isVerboseLogging: Boolean
    ) {
        if (dirs.isEmpty()) {
            return
        }

        dirs.forEach { file ->
            run {
                if (file.isDirectory) {

                    val fileUrl = file.url
                    val fileName = file.name

                    if (isVerboseLogging) {
                        writeToConsole.accept("\uD83D\uDD75\uFE0F Checking dir: '$fileUrl'")
                    }

                    if (fileUrl.matches(matchPattern) || Objects.equal(fileName, matchPattern.pattern)) {
                        ApplicationManager.getApplication().invokeLater {
                            ApplicationManager.getApplication().runWriteAction {
                                val module = findParentModule(project, arrayOf(file))
                                if (module !== null) {
                                    writeToConsole.accept("\uD83C\uDF51 Marking dir: '$fileUrl'")
                                    modifyRoots(module, arrayOf(file), isGeneratedSourceRoot)
                                }
                            }
                        }
                    } else {
                        if (isVerboseLogging) {
                            writeToConsole.accept("❌ Skipping dir: '$fileUrl'")
                        }
                    }

                    markDirs(file.children, matchPattern, writeToConsole, isGeneratedSourceRoot, isVerboseLogging);
                }
            }
        }
    }

    private fun findParentModule(project: Project?, files: Array<VirtualFile>): Module? {
        if (project == null) return null
        var result: Module? = null
        val index = ProjectFileIndex.getInstance(project)
        for (file in files) {
            val module = index.getModuleForFile(file, false) ?: return null
            if (result == null) {
                result = module
            } else if (result != module) {
                return null
            }
        }
        return result
    }


    private fun modifyRoots(module: Module, files: Array<VirtualFile>, isGeneratedSourceRoot: Boolean) {
        val model = ModuleRootManager.getInstance(module).modifiableModel
        for (file in files) {
            val entry = MarkRootActionBase.findContentEntry(model, file)
            if (entry != null) {
                val sourceFolders = entry.sourceFolders
                for (sourceFolder in sourceFolders) {
                    if (Comparing.equal(sourceFolder.file, file)) {
                        entry.removeSourceFolder(sourceFolder)
                        break
                    }
                }
                if (isGeneratedSourceRoot) {
                    modifyRoots(file, entry)
                }
            }
        }
        commitModel(module, model)
    }

    private fun modifyRoots(vFile: VirtualFile, entry: ContentEntry) {
        val properties = JpsJavaExtensionService.getInstance().createSourceRootProperties("", true)
        entry.addSourceFolder(vFile, JavaSourceRootType.SOURCE, properties)
    }

    private fun commitModel(module: Module, model: ModifiableRootModel) {
        model.commit()
        SwingUtilities.invokeLater {
            SaveAndSyncHandler.getInstance().scheduleProjectSave(module.project)
        }
    }
}
