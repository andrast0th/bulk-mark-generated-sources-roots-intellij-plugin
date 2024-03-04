package ro.nexttech.intellij.bulkmark.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.content.ContentFactory
import ro.nexttech.intellij.bulkmark.MyBundle
import ro.nexttech.intellij.bulkmark.services.MyProjectService
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JButton


class MyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class MyToolWindow(toolWindow: ToolWindow) {

        private val service = toolWindow.project.service<MyProjectService>()

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            layout = GridBagLayout()

            val lblConstraints = GridBagConstraints()
            lblConstraints.gridx = 0
            lblConstraints.gridy = 0
            val label = JBLabel(MyBundle.message("mask"));
            add(label, lblConstraints)

            val inputConstraints = GridBagConstraints()
            inputConstraints.gridx = 1
            inputConstraints.gridy = 0
            inputConstraints.fill = GridBagConstraints.HORIZONTAL
            inputConstraints.weightx = 1.0
            val input = JBTextField()
            add(input, inputConstraints)

            val btnConstraints = GridBagConstraints()
            btnConstraints.gridx = 2
            btnConstraints.gridy = 0
            val btnMarkTrue = JButton(MyBundle.message("mark"))
            add(btnMarkTrue, btnConstraints)

            val btn2Constraints = GridBagConstraints()
            btn2Constraints.gridx = 3
            btn2Constraints.gridy = 0
            val btnMarkFalse = JButton(MyBundle.message("unmark"))
            add(btnMarkFalse, btn2Constraints)

            val consoleConstraints = GridBagConstraints()
            consoleConstraints.gridx = 0
            consoleConstraints.gridy = 1
            consoleConstraints.gridwidth = 4
            consoleConstraints.fill = GridBagConstraints.BOTH
            consoleConstraints.weightx = 1.0
            consoleConstraints.weighty = 1.0

            val console = JBTextArea()
            console.isEditable = false
            val scrollPane = JBScrollPane(console)
            add(scrollPane, consoleConstraints)

            val writeToConsole: (String) -> Unit = { str -> console.text = console.text + "\n" + str }

            btnMarkTrue.apply {
                addActionListener {
                    console.text = ""
                    service.markGeneratedSourcesRoots(input.text, writeToConsole, true)
                }
            }

            btnMarkFalse.apply {
                addActionListener {
                    console.text = ""
                    service.markGeneratedSourcesRoots(input.text, writeToConsole, false)
                }
            }
        }
    }
}
