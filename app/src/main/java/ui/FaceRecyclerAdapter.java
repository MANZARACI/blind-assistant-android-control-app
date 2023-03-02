package ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blindassistant.R;

import java.util.List;

import model.Face;
import model.FaceRecyclerViewInterface;

public class FaceRecyclerAdapter extends RecyclerView.Adapter<FaceRecyclerAdapter.ViewHolder> {
    private final FaceRecyclerViewInterface faceRecyclerViewInterface;
    private Context context;
    private List<Face> faceList;

    public FaceRecyclerAdapter(Context context, List<Face> faceList,
                               FaceRecyclerViewInterface faceRecyclerViewInterface) {
        this.context = context;
        this.faceList = faceList;
        this.faceRecyclerViewInterface = faceRecyclerViewInterface;
    }

    @NonNull
    @Override
    public FaceRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.face_row, parent, false);
        return new ViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull FaceRecyclerAdapter.ViewHolder holder, int position) {
        Face face = faceList.get(position);
        holder.faceLabel.setText(face.getLabel());
    }

    @Override
    public int getItemCount() {
        return faceList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView faceLabel;
        public Button deleteBtn;

        public ViewHolder(@NonNull View itemView, Context ctx) {
            super(itemView);
            context = ctx;
            faceLabel = itemView.findViewById(R.id.faceLabelText);
            deleteBtn = itemView.findViewById(R.id.faceDeleteBtn);

            deleteBtn.setOnClickListener(view -> {
                if (faceRecyclerViewInterface != null) {
                    int pos = getAdapterPosition();

                    if (pos != RecyclerView.NO_POSITION) {
                        faceRecyclerViewInterface.onItemClick(pos);
                    }
                }
            });
        }
    }
}
