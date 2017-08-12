package de.badaix.pacetracker.views;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.maps.Job;
import de.badaix.pacetracker.maps.TilePos;
import de.badaix.pacetracker.maps.TileSourceFactory.TileSource;
import de.badaix.pacetracker.maps.TileUtils;
import de.badaix.pacetracker.session.GeoPos;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.widgets.SpinEdit;
import de.badaix.pacetracker.widgets.SpinEdit.OnValueChangedListener;

public class TileDownloadDialog extends DialogFragment implements OnClickListener, OnValueChangedListener {
    boolean mBound = false;
    private View contentView;
    private SpinEdit editMin;
    private SpinEdit editMax;
    private SpinEdit editMargin;
    private Vector<? extends GeoPos> route;
    private UpdateRunnable updateRunnable = null;
    private HashSet<TilePos> tiles = new HashSet<TilePos>();
    private OnClickListener listener = null;
    private TextView tvSources;
    private TextView tvTiles;
    private TileSource baseTileSource = TileSource.fromEnumString(GlobalSettings.getInstance().getString("MapBase", "NONE"));
    //private TileSource overlayTileSource = TileSource.fromEnumString(GlobalSettings.getInstance().getString("MapOverlay", "NONE"));
    private int layerCount = 1;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater factory = LayoutInflater.from(getActivity());
        contentView = factory.inflate(R.layout.fragment_tile_download, null);
        editMin = (SpinEdit) contentView.findViewById(R.id.editMinZoom);
        editMax = (SpinEdit) contentView.findViewById(R.id.editMaxZoom);
        editMargin = (SpinEdit) contentView.findViewById(R.id.editMargin);
        tvSources = (TextView) contentView.findViewById(R.id.tvSources);
        tvTiles = (TextView) contentView.findViewById(R.id.tvTiles);

        String text = "";
        layerCount = 0;
        if ((baseTileSource != null) && (baseTileSource.baseUrls != null)) {
            text = getString(R.string.base_tiles) + ": " + baseTileSource.name;
            editMax.setMaxValue(Math.min(baseTileSource.zoomMaxLevel, 16));
            ++layerCount;
        }
        //if ((overlayTileSource != null) && (overlayTileSource.baseUrls != null)) {
        //    text = text + "\n" + getString(R.string.overlay_tiles) + ": " + overlayTileSource.name;
        //    editMax.setMaxValue(Math.min(overlayTileSource.zoomMaxLevel, editMax.getMaxValue()));
        //    ++layerCount;
        //}
        editMin.setMaxValue(editMax.getMaxValue() - 1);

        tvSources.setText(text);
        updateRunnable = new UpdateRunnable();
        tvTiles.post(updateRunnable);

        editMin.setOnValueChangedListener(this);
        editMax.setOnValueChangedListener(this);
        editMargin.setOnValueChangedListener(this);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(R.string.tile_download).setView(contentView)
                .setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, this)
                .setInverseBackgroundForced(true);

        AlertDialog dialog = alertDialogBuilder.create();

        return dialog;
    }

    public void setTileSources(TileSource baseTileSource, TileSource overlayTileSource) {
        this.baseTileSource = baseTileSource;
        //this.overlayTileSource = overlayTileSource;
    }

    public void setOnClickListener(OnClickListener listener) {
        this.listener = listener;
    }

    public void setRoute(Vector<? extends GeoPos> route) {
        this.route = route;
    }

    private void updateTiles() {
        if (this.route != null) {
            int maxZoom = editMax.getValue();
            int minZoom = editMin.getValue();
            tiles = TileUtils.routeToTile(route, maxZoom);
            tiles = TileUtils.addMargin(tiles, editMargin.getValue());
            HashSet<TilePos> coarseTiles = tiles;
            Hint.log(this, "Tiles at zoom level " + maxZoom + ": " + tiles.size());
            for (int i = maxZoom - 1; i >= minZoom; --i) {
                coarseTiles = TileUtils.zoomOut(coarseTiles);
                Hint.log(this, "Tiles at zoom level " + i + ": " + coarseTiles.size());
                tiles.addAll(coarseTiles);
            }
            Hint.log(this, "Tiles overall: " + tiles.size() + ", ~" + (633. / 1000.) * tiles.size() + "KB");
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (listener != null)
            listener.onClick(dialog, which);
    }

    @Override
    public void onValueChanged(SpinEdit spinEdit, int newValue) {
        if (spinEdit == editMax)
            editMin.setValue(Math.min(editMin.getValue(), editMax.getValue() - 1));
        else if (spinEdit == editMin)
            editMax.setValue(Math.max(editMin.getValue() + 1, editMax.getValue()));
        tvTiles.removeCallbacks(updateRunnable);
        updateRunnable = new UpdateRunnable();
        tvTiles.post(updateRunnable);
    }

    public HashSet<TilePos> getTiles() {
        return tiles;
    }

    public Job[] getJobs() {
        Job[] result = new Job[layerCount];
        int i = 0;
        if ((baseTileSource != null) && (baseTileSource.baseUrls != null)) {
            result[i++] = new Job(tiles, baseTileSource);
        }
        //if ((overlayTileSource != null) && (overlayTileSource.baseUrls != null)) {
        //    result[i++] = new Job(tiles, overlayTileSource);
        //}
        return result;
    }

    //public TileSource getOverlayTileSource() {
    //    return overlayTileSource;
    //}

    private class UpdateRunnable implements Runnable {
        @Override
        public void run() {
            updateTiles();
            tvTiles.setText(getString(R.string.tiles) + ": " + layerCount * tiles.size());
            if (layerCount * tiles.size() > 2000)
                editMax.setValue(editMax.getValue() - 1);
            // onValueChanged(editMax, editMax.getValue() - 1);
        }
    }

}
