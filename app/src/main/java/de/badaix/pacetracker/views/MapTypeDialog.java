package de.badaix.pacetracker.views;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.maps.TileSourceFactory.TileSource;

public class MapTypeDialog extends DialogFragment implements OnClickListener, OnItemSelectedListener {
    private View contentView;
    private Spinner spinnerBase;
    //private Spinner spinnerOverlay;
    private OnClickListener listener = null;
    private TileSource baseTileSource = TileSource.GOOGLETERRAIN;
    private TileSource overlayTileSource = TileSource.NONE;
    private ArrayAdapter<TileSource> baseAdapter;
    private ArrayAdapter<TileSource> overlayAdapter;

    // private OfflineDownloader downloader = new OfflineDownloader();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater factory = LayoutInflater.from(getActivity());
        contentView = factory.inflate(R.layout.fragment_map_type, null);
        spinnerBase = (Spinner) contentView.findViewById(R.id.spinnerBase);
        //spinnerOverlay = (Spinner) contentView.findViewById(R.id.spinnerOverlay);
        baseAdapter = new ArrayAdapter<TileSource>(getActivity(), android.R.layout.simple_spinner_item);
        baseAdapter.add(TileSource.GOOGLE);
        //baseAdapter.add(TileSource.GOOGLE_BITMAP);
        baseAdapter.add(TileSource.GOOGLESATELLITE);
        baseAdapter.add(TileSource.GOOGLETERRAIN);
        // baseAdapter.add(TileSource.MAPNIK);
        // baseAdapter.add(TileSource.MAPQUESTOSM);
        // baseAdapter.add(TileSource.MAPQUESTOPENAERIAL);
        // baseAdapter.add(TileSource.CYCLEMAP);
        // baseAdapter.add(TileSource.OSMPUBLICTRANSPORT);
        // baseAdapter.add(TileSource.WATERCOLOR);
        // baseAdapter.add(TileSource.NOKIA);
        baseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBase.setAdapter(baseAdapter);
        spinnerBase.setOnItemSelectedListener(this);
        spinnerBase.setSelection(baseAdapter.getPosition(baseTileSource));

        //overlayAdapter = new ArrayAdapter<TileSource>(getActivity(), android.R.layout.simple_spinner_item);
        //overlayAdapter.add(TileSource.NONE);
        //overlayAdapter.add(TileSource.SEAMARK);
        //// overlayAdapter.add(TileSource.GEOCACHING);
        //overlayAdapter.add(TileSource.OPENPISTEMAP);
        //overlayAdapter.add(TileSource.OPENPISTEMAPSHADED);
        //overlayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //spinnerOverlay.setAdapter(overlayAdapter);
        //spinnerOverlay.setOnItemSelectedListener(this);
        //spinnerOverlay.setSelection(overlayAdapter.getPosition(overlayTileSource));

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(R.string.map_type).setView(contentView)
                .setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, this)
                .setInverseBackgroundForced(true);

        AlertDialog dialog = alertDialogBuilder.create();
        return dialog;
    }

    public void setOnClickListener(OnClickListener listener) {
        this.listener = listener;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (listener != null)
            listener.onClick(dialog, which);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == spinnerBase)
            baseTileSource = baseAdapter.getItem(position);
        //else if (parent == spinnerOverlay)
        //    overlayTileSource = overlayAdapter.getItem(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    public TileSource getBaseTileSource() {
        return baseTileSource;
    }

    public void setBaseTileSource(TileSource baseTileSource) {
        this.baseTileSource = baseTileSource;
    }

    public TileSource getOverlayTileSource() {
        return overlayTileSource;
    }

    public void setOverlayTileSource(TileSource overlayTileSource) {
        this.overlayTileSource = overlayTileSource;
    }

}
