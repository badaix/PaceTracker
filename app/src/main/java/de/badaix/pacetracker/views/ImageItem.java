package de.badaix.pacetracker.views;

import de.badaix.pacetracker.R;

public class ImageItem // extends LinearLayout
{

    // private TextView tv = null;
    private String title;
    private int drawable;

    public ImageItem(String title) {
        setTitle(title);
        setDrawable(R.drawable.icon);
    }

    public ImageItem(String title, int drawable) {
        setTitle(title);
        setDrawable(drawable);
    }

    public int getDrawable() {
        return drawable;
    }

    public void setDrawable(int drawable) {
        this.drawable = drawable;
    }

	/*
     * public ImageItem(Context context, ViewGroup parent) { super(context); //
	 * TODO Auto-generated constructor stub // LayoutInflater inflater =
	 * (LayoutInflater
	 * )context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	 * LinearLayout.inflate(context, R.layout.imageitem, parent); tv =
	 * (TextView)findViewById(R.id.TextViewContactName); }
	 */

    public String getTitle() {
        return title;
    }

    public void setTitle(String text) {
        title = text;
    }
}
