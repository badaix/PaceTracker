package de.badaix.pacetracker.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.Point;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.ImageView;

public class Compass {
    protected final Picture mCompassFrame = new Picture();
    protected final Picture mCompassRose = new Picture();
    protected final float COMPASS_FRAME_CENTER_X;
    protected final float COMPASS_FRAME_CENTER_Y;
    protected final float COMPASS_ROSE_CENTER_X;
    protected final float COMPASS_ROSE_CENTER_Y;
    private final Matrix mCompassMatrix = new Matrix();
    private final Display mDisplay;
    /**
     * The bearing, in degrees east of north, or NaN if none has been set.
     */
    private final float mCompassRadius = 20.0f;
    private float mScale = 1.0f;

    public Compass(final Context ctx) {
        final WindowManager windowManager = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);

        mDisplay = windowManager.getDefaultDisplay();

        createCompassFramePicture();
        createCompassRosePicture();

        COMPASS_FRAME_CENTER_X = mCompassFrame.getWidth() / 2 - 0.5f;
        COMPASS_FRAME_CENTER_Y = mCompassFrame.getHeight() / 2 - 0.5f;
        COMPASS_ROSE_CENTER_X = mCompassRose.getWidth() / 2 - 0.5f;
        COMPASS_ROSE_CENTER_Y = mCompassRose.getHeight() / 2 - 0.5f;
    }

    public void drawCompass(ImageView imageView, int bearing) {
        bearing += getDisplayOrientation();
        int width = imageView.getWidth() - imageView.getPaddingLeft() - imageView.getPaddingRight();
        int height = imageView.getHeight() - imageView.getPaddingTop() - imageView.getPaddingBottom();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        final float centerX = height / 2;
        final float centerY = width / 2;

        mCompassMatrix.setTranslate(-COMPASS_FRAME_CENTER_X, -COMPASS_FRAME_CENTER_Y);
        mCompassMatrix.postTranslate(centerX, centerY);

        canvas.save();
        canvas.setMatrix(mCompassMatrix);
        canvas.drawPicture(mCompassFrame);

        mCompassMatrix.setRotate(-bearing, COMPASS_ROSE_CENTER_X, COMPASS_ROSE_CENTER_Y);
        mCompassMatrix.postTranslate(-COMPASS_ROSE_CENTER_X, -COMPASS_ROSE_CENTER_Y);
        mCompassMatrix.postTranslate(centerX, centerY);
        canvas.setMatrix(mCompassMatrix);
        canvas.drawPicture(mCompassRose);

        canvas.restore();
        imageView.setImageBitmap(bitmap);
    }

    public int getDisplayOrientation() {
        switch (mDisplay.getOrientation()) {
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            default:
                return 0;
        }
    }

    private Point calculatePointOnCircle(final float centerX, final float centerY, final float radius,
                                         final float degrees) {
        // for trigonometry, 0 is pointing east, so subtract 90
        // compass degrees are the wrong way round
        final double dblRadians = Math.toRadians(-degrees + 90);

        final int intX = (int) (radius * Math.cos(dblRadians));
        final int intY = (int) (radius * Math.sin(dblRadians));

        return new Point((int) centerX + intX, (int) centerY - intY);
    }

    private void drawTriangle(final Canvas canvas, final float x, final float y, final float radius,
                              final float degrees, final Paint paint) {
        canvas.save();
        final Point point = this.calculatePointOnCircle(x, y, radius, degrees);
        canvas.rotate(degrees, point.x, point.y);
        final Path p = new Path();
        p.moveTo(point.x - 2 * mScale, point.y);
        p.lineTo(point.x + 2 * mScale, point.y);
        p.lineTo(point.x, point.y - 5 * mScale);
        p.close();
        canvas.drawPath(p, paint);
        canvas.restore();
    }

    private void createCompassFramePicture() {
        // The inside of the compass is white and transparent
        final Paint innerPaint = new Paint();
        innerPaint.setColor(Color.WHITE);
        innerPaint.setAntiAlias(true);
        innerPaint.setStyle(Style.FILL);
        innerPaint.setAlpha(200);

        // The outer part (circle and little triangles) is gray and transparent
        final Paint outerPaint = new Paint();
        outerPaint.setColor(Color.GRAY);
        outerPaint.setAntiAlias(true);
        outerPaint.setStyle(Style.STROKE);
        outerPaint.setStrokeWidth(2.0f);
        outerPaint.setAlpha(200);

        final int picBorderWidthAndHeight = (int) ((mCompassRadius + 5) * 2);
        final int center = picBorderWidthAndHeight / 2;

        final Canvas canvas = mCompassFrame.beginRecording(picBorderWidthAndHeight, picBorderWidthAndHeight);

        // draw compass inner circle and border
        canvas.drawCircle(center, center, mCompassRadius * mScale, innerPaint);
        canvas.drawCircle(center, center, mCompassRadius * mScale, outerPaint);

        // Draw little triangles north, south, west and east (don't move)
        // to make those move use "-bearing + 0" etc. (Note: that would mean to
        // draw the triangles
        // in the onDraw() method)
        drawTriangle(canvas, center, center, mCompassRadius * mScale, 0, outerPaint);
        drawTriangle(canvas, center, center, mCompassRadius * mScale, 90, outerPaint);
        drawTriangle(canvas, center, center, mCompassRadius * mScale, 180, outerPaint);
        drawTriangle(canvas, center, center, mCompassRadius * mScale, 270, outerPaint);

        mCompassFrame.endRecording();
    }

    private void createCompassRosePicture() {
        // Paint design of north triangle (it's common to paint north in red
        // color)
        final Paint northPaint = new Paint();
        northPaint.setColor(0xFFA00000);
        northPaint.setAntiAlias(true);
        northPaint.setStyle(Style.FILL);
        northPaint.setAlpha(220);

        // Paint design of south triangle (black)
        final Paint southPaint = new Paint();
        southPaint.setColor(Color.BLACK);
        southPaint.setAntiAlias(true);
        southPaint.setStyle(Style.FILL);
        southPaint.setAlpha(220);

        // Create a little white dot in the middle of the compass rose
        final Paint centerPaint = new Paint();
        centerPaint.setColor(Color.WHITE);
        centerPaint.setAntiAlias(true);
        centerPaint.setStyle(Style.FILL);
        centerPaint.setAlpha(220);

        // final int picBorderWidthAndHeight = (int) ((mCompassRadius + 5) * 2 *
        // mScale);
        final int picBorderWidthAndHeight = (int) ((mCompassRadius + 5) * 2);
        final int center = picBorderWidthAndHeight / 2;

        final Canvas canvas = mCompassRose.beginRecording(picBorderWidthAndHeight, picBorderWidthAndHeight);

        // Blue triangle pointing north
        final Path pathNorth = new Path();
        pathNorth.moveTo(center, center - (mCompassRadius - 3) * mScale);
        pathNorth.lineTo(center + 4 * mScale, center);
        pathNorth.lineTo(center - 4 * mScale, center);
        pathNorth.lineTo(center, center - (mCompassRadius - 3) * mScale);
        pathNorth.close();
        canvas.drawPath(pathNorth, northPaint);

        // Red triangle pointing south
        final Path pathSouth = new Path();
        pathSouth.moveTo(center, center + (mCompassRadius - 3) * mScale);
        pathSouth.lineTo(center + 4 * mScale, center);
        pathSouth.lineTo(center - 4 * mScale, center);
        pathSouth.lineTo(center, center + (mCompassRadius - 3) * mScale);
        pathSouth.close();
        canvas.drawPath(pathSouth, southPaint);

        // Draw a little white dot in the middle
        canvas.drawCircle(center, center, 2, centerPaint);

        mCompassRose.endRecording();
    }
}
