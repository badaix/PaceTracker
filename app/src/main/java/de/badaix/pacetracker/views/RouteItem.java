package de.badaix.pacetracker.views;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.net.URL;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.maps.BaseTileSource;
import de.badaix.pacetracker.maps.TilePos;
import de.badaix.pacetracker.maps.TileSourceFactory;
import de.badaix.pacetracker.maps.TileSourceFactory.TileSource;
import de.badaix.pacetracker.maps.TileUtils;
import de.badaix.pacetracker.session.RouteInfo;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.social.UrlImageViewHelper;
import de.badaix.pacetracker.util.Distance;

public class RouteItem extends LinearLayout implements OnClickListener {
    private TextView tvRouteName;
    private TextView tvRouteFromTo;
    private TextView tvRouteDistance;
    private ImageView imageViewMap;
    private ImageView imageViewType;
    private RouteInfo routeInfo;
    private Context context;

    public RouteItem(Context context) {
        this(context, null);
    }

    public RouteItem(Context context, RouteInfo routeInfo) {
        super(context);
        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        vi.inflate(R.layout.route_item, this);
        tvRouteName = (TextView) findViewById(R.id.tvName);
        tvRouteFromTo = (TextView) findViewById(R.id.tvFromTo);
        tvRouteDistance = (TextView) findViewById(R.id.tvDistance);
        imageViewType = (ImageView) findViewById(R.id.imageViewType);
        imageViewMap = (ImageView) findViewById(R.id.imageViewMap);
        this.context = context;

        setRouteInfo(routeInfo);
    }

    public void update() {
        if (routeInfo == null)
            return;

        tvRouteName.setText(routeInfo.getName());
        tvRouteFromTo.setText(routeInfo.getDescription());
        if (TextUtils.isEmpty(routeInfo.getDescription()))
            tvRouteFromTo.setText(routeInfo.getName());

        tvRouteDistance.setText(Distance.distanceToString(routeInfo.getDistance(), 2) + " "
                + GlobalSettings.getInstance(context).getDistUnit().toShortString());
        // <item>"fastest"</item>
        // <item>"shortest"</item>
        // <item>"pedestrian"</item>
        // <item>"multimodal"</item>
        // <item>"bicycle"</item>
        if (routeInfo.getType().equalsIgnoreCase("fastest") || routeInfo.getType().equalsIgnoreCase("shortest"))
            imageViewType.setImageResource(R.drawable.car);
        else if (routeInfo.getType().equalsIgnoreCase("pedestrin") || routeInfo.getType().equalsIgnoreCase("running"))
            imageViewType.setImageResource(R.drawable.running);
        else if (routeInfo.getType().equalsIgnoreCase("bicycle") || routeInfo.getType().equalsIgnoreCase("Cycling"))
            imageViewType.setImageResource(R.drawable.cycling);
        else if (routeInfo.getType().equalsIgnoreCase("walking"))
            imageViewType.setImageResource(R.drawable.hiking);
        else
            imageViewType.setImageResource(R.drawable.transparent);
        if (routeInfo.getEndPos() != null) {
            BaseTileSource tileSource = TileSourceFactory.getTileSource(TileSource.GOOGLE_BITMAP);
            TilePos tilePos = TileUtils.getTileNumber(routeInfo.getEndPos(), 18);
            URL tileUrl = tileSource.getTileURL(0, (int) tilePos.x, (int) tilePos.y, tilePos.z);
            UrlImageViewHelper.getInstance().setUrlDrawable(imageViewMap, tileUrl.toExternalForm(),
                    R.drawable.ic_menu_mapmode, null);
        }

    }

    public RouteInfo getRouteInfo() {
        return routeInfo;
    }

    public void setRouteInfo(RouteInfo routeInfo) {
        this.routeInfo = routeInfo;
        update();
    }

    @Override
    public void onClick(View v) {
    }
}
