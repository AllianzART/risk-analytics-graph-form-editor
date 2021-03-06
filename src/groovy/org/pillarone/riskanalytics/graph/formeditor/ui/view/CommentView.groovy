package org.pillarone.riskanalytics.graph.formeditor.ui.view

import com.ulcjava.base.application.event.ActionEvent
import com.ulcjava.base.application.event.IActionListener
import com.ulcjava.base.application.util.HTMLUtilities
import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.graph.model.Connection
import org.pillarone.riskanalytics.graph.core.graph.model.filters.IComponentNodeFilter
import org.pillarone.riskanalytics.graph.formeditor.ui.ISelectionListener
import org.pillarone.riskanalytics.graph.formeditor.ui.model.treetable.NodeNameFilter
import org.pillarone.riskanalytics.graph.formeditor.util.UIUtils
import com.ulcjava.base.application.*

/**
 * @author fouad.jaada@intuitive-collaboration.com, martin.melchior@fhnw.ch
 */
class CommentView implements ISelectionListener {

    ULCBoxPane content
    ULCHtmlPane propertiesPane
    ULCBoxPane buttonPane
    ULCCardPane descriptionPane
    ResourceLinkHtmlPane htmlTextPane
    ULCTextArea editableTextPane
    ULCButton editButton
    String currentText
    ComponentNode currentNode
    private AbstractGraphModel graphModel

    private boolean readOnly = false;

    public CommentView(boolean readOnly) {
        this.readOnly = readOnly
        init()
        attachListeners()
    }

    public void setGraphModel(AbstractGraphModel graphModel) {
        this.graphModel = graphModel
    }

    private void init() {
        content = new ULCBoxPane(true)
        propertiesPane = new ULCHtmlPane()

        descriptionPane = new ULCCardPane()
        descriptionPane.setBorder(BorderFactory.createTitledBorder("Comment"))
        htmlTextPane = new ResourceLinkHtmlPane()
        editableTextPane = new ULCTextArea("...")
        editableTextPane.setEditable(true)
        descriptionPane.addCard("text", editableTextPane)
        descriptionPane.addCard("html", htmlTextPane)

        buttonPane = new ULCBoxPane(false)
        editButton = new ULCButton("edit");
        editButton.setEnabled false
        editButton.setToolTipText("Edit the comment.");
        ULCButton okButton = new ULCButton("ok");
        okButton.setEnabled false

        ULCButton cancelButton = new ULCButton("cancel");
        cancelButton.setEnabled false

        editButton.addActionListener(
                new IActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                        editableTextPane.setText(currentText)
                        descriptionPane.setSelectedComponent(editableTextPane);
                        editButton.setEnabled false
                        okButton.setEnabled true
                        cancelButton.setEnabled true
                    }
                }
        )
        IActionListener okAction = new IActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                currentText = editableTextPane.getText()
                currentNode.comment = currentText
                String htmlText = "<div style='100%'> $currentText </div>"
                String html = UIUtils.convertWikiToHtml(htmlText)
                htmlTextPane.setText(html)
                descriptionPane.setSelectedComponent(htmlTextPane);
                editButton.setEnabled true
                okButton.setEnabled false
                cancelButton.setEnabled false
            }
        }
        okButton.addActionListener(okAction)

        IActionListener cancelAction = new IActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                currentText = currentNode.comment
                descriptionPane.setSelectedComponent(htmlTextPane);
                editButton.setEnabled true
                okButton.setEnabled false
                cancelButton.setEnabled false
            }
        }
        cancelButton.addActionListener(cancelAction)
        buttonPane.add(ULCBoxPane.BOX_LEFT_CENTER, editButton)
        buttonPane.add(ULCBoxPane.BOX_EXPAND_CENTER, ULCFiller.createHorizontalGlue())
        buttonPane.add(ULCBoxPane.BOX_LEFT_CENTER, cancelButton)
        buttonPane.add(ULCBoxPane.BOX_LEFT_CENTER, okButton)
        content.add(ULCBoxPane.BOX_EXPAND_TOP, propertiesPane)
        content.add(ULCBoxPane.BOX_EXPAND_EXPAND, descriptionPane)
        content.add(ULCBoxPane.BOX_LEFT_BOTTOM, buttonPane)
    }

    private void attachListeners() {
    }

    public void setSelectedComponents(List<ComponentNode> selectedNodes) {
        currentNode = this.getSelectedTopLevelNode(selectedNodes)
        boolean isEditable = true
        if (!currentNode) {
            isEditable = false
            currentNode = selectedNodes != null && selectedNodes.size() > 0 ? selectedNodes[0] : null
        }
        if (currentNode) {
            editButton.setEnabled(!readOnly && isEditable)

            // properties pane
            String title = org.pillarone.riskanalytics.graph.core.graph.util.UIUtils.formatDisplayName(currentNode.getName())
            //String fullName = currentNode.type.getTypeClass().getName()
            String text = "<<h3> $title </h3>"
            propertiesPane.setText(HTMLUtilities.convertToHtml(text))

            // description pane
            currentText = currentNode.comment
            if (currentText == null || currentText == "") {
                currentText = "No comments yet"
            }
            String htmlText = "$currentText"
            // String html = UIUtils.convertWikiToHtml(htmlText)
            String html = HTMLUtilities.convertToHtml(htmlText)
            htmlTextPane.setText(html)
            descriptionPane.setSelectedComponent(htmlTextPane)
        }
    }

    private ComponentNode getSelectedTopLevelNode(List<ComponentNode> selectedNodes) {
        if (selectedNodes != null) {
            for (ComponentNode node: selectedNodes) {
                if (graphModel.getAllComponentNodes().contains(node)) {
                    return node
                }
            }
        }
        return null
    }

    public void setSelectedConnections(List<Connection> selectedConnections) {
        // nothing to do here
    }

    public void applyFilter(IComponentNodeFilter filter) {
        // nothing to do here
    }

    public void applyFilter(NodeNameFilter filter) {
    }



    public void clearSelection() {
        // properties pane
        propertiesPane.setText ""

        // description pane
        currentText = ""
        htmlTextPane.setText currentText
        descriptionPane.setComponentAt("html", htmlTextPane)
    }
}

