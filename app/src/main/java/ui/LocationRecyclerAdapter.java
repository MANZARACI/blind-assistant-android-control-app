package ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blindassistant.R;
import com.google.maps.android.PolyUtil;

import java.util.List;

import model.Location;
import model.LocationRecyclerViewInterface;
import util.Settings;

public class LocationRecyclerAdapter extends RecyclerView.Adapter<LocationRecyclerAdapter.ViewHolder> {
    private final LocationRecyclerViewInterface locationRecyclerViewInterface;
    private Context context;
    private List<Location> locationList;

    public LocationRecyclerAdapter(Context context, List<Location> locationList,
                                   LocationRecyclerViewInterface locationRecyclerViewInterface) {
        this.context = context;
        this.locationList = locationList;
        this.locationRecyclerViewInterface = locationRecyclerViewInterface;
    }

    @NonNull
    @Override
    public LocationRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.location_row, parent, false);
        return new ViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationRecyclerAdapter.ViewHolder holder, int position) {
        Location location = locationList.get(position);

        holder.timeText.setText(location.getTime().getTimeString());
        holder.dateText.setText(location.getTime().getDateString());

        boolean isInside = PolyUtil.containsLocation(location.getLat(), location.getLng(), holder.settings.getBoundaryPoints(), true);
        if (!isInside) {
            holder.boundaryOutsideText.setVisibility(View.VISIBLE);
        } else {
            holder.boundaryOutsideText.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return locationList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView timeText, dateText, boundaryOutsideText;
        public Settings settings;

        public ViewHolder(@NonNull View itemView, Context ctx) {
            super(itemView);
            context = ctx;
            timeText = itemView.findViewById(R.id.locationTimeText);
            dateText = itemView.findViewById(R.id.locationDateText);
            boundaryOutsideText = itemView.findViewById(R.id.boundaryOutsideText);

            settings = Settings.getInstance();

            itemView.setOnClickListener(view -> {
                if (locationRecyclerViewInterface != null) {
                    int pos = getAdapterPosition();

                    if (pos != RecyclerView.NO_POSITION) {
                        locationRecyclerViewInterface.onItemClick(pos);
                    }
                }
            });
        }
    }

}
