package ro.nexttech.intellij.bulkmark.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.*
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import org.jetbrains.annotations.NotNull
import ro.nexttech.intellij.bulkmark.MyBundle
import ro.nexttech.intellij.bulkmark.services.MyProjectService
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingUtilities


class MyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class MyToolWindow(toolWindow: ToolWindow) {

        private val service = toolWindow.project.service<MyProjectService>()
        private val project = toolWindow.project;

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            layout = GridBagLayout()
            border = JBUI.Borders.empty(10)

            // ROW 1
            val lblConstraints = GridBagConstraints()
            lblConstraints.gridx = 0
            lblConstraints.gridy = 0
            val label = JBLabel(MyBundle.message("mask"));
            add(label, lblConstraints)

            val inputConstraints = GridBagConstraints()
            inputConstraints.gridx = 1
            inputConstraints.gridy = 0
            inputConstraints.gridwidth = 2
            inputConstraints.fill = GridBagConstraints.HORIZONTAL
            inputConstraints.weightx = 1.0
            val input = JBTextField()
            input.emptyText.text = "For ex: \".*/target/noci-generated-sources\""
            add(input, inputConstraints)

            // ROW 2
            val checkboxConstraints = GridBagConstraints()
            checkboxConstraints.gridx = 0
            checkboxConstraints.gridy = 1
            checkboxConstraints.fill = GridBagConstraints.HORIZONTAL
            val checkBoxVerbose = JBCheckBox(MyBundle.message("verbose"))
            add(checkBoxVerbose, checkboxConstraints)

            val btnPanelConstraints = GridBagConstraints()
            btnPanelConstraints.gridx = 1
            btnPanelConstraints.gridy = 1
            btnPanelConstraints.fill = GridBagConstraints.HORIZONTAL
            btnPanelConstraints.weightx = 1.0
            val btnPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
            add(btnPanel, btnPanelConstraints)

            val btnMarkTrue = JButton(MyBundle.message("mark"))
            btnPanel.add(btnMarkTrue)
            val btnMarkFalse = JButton(MyBundle.message("unmark"))
            btnPanel.add(btnMarkFalse)

            // ROW 3
            val consoleConstraints = GridBagConstraints()
            consoleConstraints.gridx = 0
            consoleConstraints.gridy = 2
            consoleConstraints.gridwidth = 3
            consoleConstraints.fill = GridBagConstraints.BOTH
            consoleConstraints.weightx = 1.0
            consoleConstraints.weighty = 1.0
            val console = JBTextArea()
            console.isEditable = false

            val scrollPane = JBScrollPane(console)
            add(scrollPane, consoleConstraints)

            val writeToConsole: (String) -> Unit = { str ->
                SwingUtilities.invokeLater { console.text = console.text + "\n" + str }
            }

            btnMarkTrue.apply {
                addActionListener {
                    ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Marking dirs...", false) {
                        override fun run(@NotNull progressIndicator: ProgressIndicator) {
                            console.text = ""
                            service.markGeneratedSourcesRoots(
                                input.text,
                                writeToConsole,
                                true,
                                checkBoxVerbose.isSelected
                            )
                        }
                    })
                }
            }

            btnMarkFalse.apply {
                addActionListener {
                    ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Unmarking dirs...") {
                        override fun run(@NotNull progressIndicator: ProgressIndicator) {
                            console.text = ""
                            service.markGeneratedSourcesRoots(
                                input.text,
                                writeToConsole,
                                false,
                                checkBoxVerbose.isSelected
                            )
                        }
                    })
                }
            }

            btnMarkFalse.requestFocus()
        }
    }
}
