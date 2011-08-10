package org.pillarone.riskanalytics.graph.formeditor.ui.model.treetable

import com.ulcjava.base.application.tabletree.IMutableTableTreeNode
import com.ulcjava.base.application.tabletree.ITableTreeNode
import org.pillarone.riskanalytics.core.simulation.item.parameter.comment.Comment
/**
 * Copied from RiskAnalyticsApplication - can be replaced once this is available in a separate, accessible plug-in
 * Commented out references to the I18NUtils and ComponentUtils.
 */
class SimpleTableTreeNode implements IMutableTableTreeNode {
    boolean expanded = true
    protected String name, cachedDisplayName
    protected String cachedToolTip
    List<ITableTreeNode> children
    ITableTreeNode parent
    Set comments = new HashSet()

    static String PATH_SEPARATOR = '/'
    static String TOOLTIP = ".tooltip"

    public SimpleTableTreeNode(String name) {
        children = []
        this.@name = name
    }

    String getName() {
        this.@name
    }

    public ITableTreeNode getChildAt(int index) {
        children[index]
    }

    public int getChildCount() {
        children.size()
    }

    public ITableTreeNode getParent() {
        parent
    }

    public int getIndex(ITableTreeNode child) {
        children.indexOf(child)
    }

    public Object getValueAt(int i) {
        if (i == 0) {
            return getDisplayName()
        } else {
            return getCellValue(i)
        }
    }

    public String getDisplayName() {
        if (cachedDisplayName != null)
            return cachedDisplayName
        String value = name
        String displayNameValue = null //lookUp(value, "")
        if (displayNameValue == null)
            // displayNameValue = org.pillarone.riskanalytics.application.ui.util.I18NUtils.formatDisplayName(value)
            displayNameValue = value
        if (displayNameValue != null) {
            cachedDisplayName = displayNameValue
        }
        return displayNameValue

    }

    /*private String lookUp(String value, String tooltip = "") {
        //try to get a displayValue from the parent.component
        String displayNameValue = org.pillarone.riskanalytics.application.ui.util.I18NUtils.findResultParameterDisplayName(this, value, tooltip)
        if (displayNameValue == null)
            displayNameValue = org.pillarone.riskanalytics.application.ui.util.I18NUtils.findDisplayNameByParentComponent(this, value, tooltip)
        if (displayNameValue == null)
            displayNameValue = org.pillarone.riskanalytics.application.ui.util.I18NUtils.findDisplayNameByParentComponent(this.parent, value, tooltip)
        return displayNameValue
    }*/

    public String getToolTip() {
        if (!cachedToolTip) {
            String value = name
            cachedToolTip = lookUp(value, TOOLTIP)
        }
        return cachedToolTip
    }

    protected Object getCellValue(int column) {
        if (expanded) {
            return getExpandedCellValue(column)
        }
        return getCollapsedCellValue(column)
    }

    public boolean isLeaf() {
        children.size() == 0
    }

    String getPath() {
        if (parent) {
            return "${parent?.path}:$name"
        } else {
            return name
        }
    }

    String getDisplayPath() {
        if (parent) {
            return "${parent.getDisplayPath()} $PATH_SEPARATOR ${getDisplayName()}"
        } else {
            return getDisplayName()
        }
    }

    List getTreePath() {
        if (parent) {
            return parent.treePath << this
        } else {
            return [this]
        }
    }

    /**String getShortDisplayPath(List otherNodes) {
        if (!otherNodes.contains(this)) otherNodes.add(this)
        List otherNodesCopy = otherNodes.clone()
        List list = intersection(otherNodesCopy.treePath)
        if (list && list.size() > 0 && list.last().displayPath != displayPath) {
            return displayPath[(list.last().displayPath.size() + 3)..(displayPath.size() - 1)]
        } else {
            if (otherNodes.size() == 1 && otherNodes[0] == this) {
                return displayName
            } else {
                return displayPath
            }
        }
    }*/

    int add(ITableTreeNode child) {
        children << child
        child.parent = this
        return childCount - 1
    }

    int remove(ITableTreeNode child) {
        int index = getIndex(child)
        children.remove(index)
        child.parent = null
        return index
    }

    public void setValueAt(Object o, int i) {
        throw new UnsupportedOperationException("Not implemented on simple node")
    }

    public void insert(IMutableTableTreeNode child, int index) {
        children.add(index, child)
        child.parent = this
    }

    public void remove(int childIndex) {
        remove(getChildAt(childIndex))

    }

    public void setParent(IMutableTableTreeNode parent) {
        this.parent = parent
    }

    public boolean isCellEditable(int i) {
        return false
    }


    Object getExpandedCellValue(int column) {
        ""
    }

    Object getCollapsedCellValue(int column) {
        StringBuffer childValues = new StringBuffer()
        children.each {
            def cellValue = it.getExpandedCellValue(column)
            if (cellValue) {
                childValues << cellValue
                childValues << " "
            }
        }
        return childValues.toString().trim()
    }

    SimpleTableTreeNode getChildByName(String name) {
        return children.find {it.name == name}
    }

    String getCommentMessage() {
        StringBuilder sb = new StringBuilder("")
        for (Comment comment: comments) {
            sb.append(comment.getText() + "\n")
        }
        return sb.toString()
    }
}