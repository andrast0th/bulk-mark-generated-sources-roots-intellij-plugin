package ro.nexttech.intellij.bulkmark.services

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
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import java.util.function.Consumer


@Service(Service.Level.PROJECT)
class MyProjectService(private val project: Project) {

    fun markGeneratedSourcesRoots(matchPattern: String, writeToConsole: Consumer<String>, isGeneratedSourceRoot: Boolean) {

        val regex = matchPattern.toRegex()
        writeToConsole.accept("Searching dirs matching mask: $regex")

        val allModuleDirs = ProjectRootManager.getInstance(project).contentRoots
        val rootDirs = findRootDirectories(allModuleDirs.toList()).toArray(emptyArray())

        markDirs(rootDirs, regex, writeToConsole, isGeneratedSourceRoot)

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

    private fun markDirs(dirs: Array<VirtualFile>, matchPattern: Regex, writeToConsole: Consumer<String>, isGeneratedSourceRoot: Boolean) {
        if (dirs.isEmpty()) {
            return
        }

        dirs.forEach { file ->
            run {
                if (file.isDirectory) {
                    val fileUrl = file.url
                    if (fileUrl.matches(matchPattern)) {
                        val module = findParentModule(project, arrayOf(file))
                        if(module !== null) {
                            writeToConsole.accept("Marking: $fileUrl")
                            modifyRoots(module, arrayOf(file), isGeneratedSourceRoot)
                        }
                    }
                    markDirs(file.children, matchPattern, writeToConsole, isGeneratedSourceRoot);
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
                if(isGeneratedSourceRoot) {
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
        ApplicationManager.getApplication().runWriteAction { model.commit() }
        SaveAndSyncHandler.getInstance().scheduleProjectSave(module.project)
    }
}
