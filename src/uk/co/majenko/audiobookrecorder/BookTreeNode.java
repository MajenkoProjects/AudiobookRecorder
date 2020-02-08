package uk.co.majenko.audiobookrecorder;

import javax.swing.tree.DefaultMutableTreeNode;

public abstract class BookTreeNode extends DefaultMutableTreeNode {

    public BookTreeNode(String t) {
        super(t);
    }

    public BookTreeNode() {
        super("");
    }

    public abstract void setNotes(String t);
    public abstract String getNotes();

    public abstract void onSelect(BookTreeNode target);
    public abstract Book getBook();
}

