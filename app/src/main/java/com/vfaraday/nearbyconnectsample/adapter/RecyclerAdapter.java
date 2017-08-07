package com.vfaraday.nearbyconnectsample.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.vfaraday.nearbyconnectsample.P2PStarConnectionActivity.Endpoint;
import com.vfaraday.nearbyconnectsample.R;

import java.util.ArrayList;
import java.util.Set;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.EndpointHolder> {

    public void setEndpoints(Set<Endpoint> endpointList) {
        endpoints.addAll(endpointList);
    }

    private ArrayList<Endpoint> endpoints;

    public RecyclerAdapter(Set<Endpoint> endpointList) {
        endpoints.addAll(endpointList);
    }

    @Override
    public EndpointHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_item, parent, false);

        return new EndpointHolder(view);
    }

    @Override
    public void onBindViewHolder(EndpointHolder holder, int position) {
        holder.endpointId.setText(endpoints.get(position).getId());
        holder.endpointName.setText(endpoints.get(position).getName());
    }

    @Override
    public int getItemCount() {
        return endpoints.size();
    }

    class EndpointHolder extends RecyclerView.ViewHolder{

        private TextView endpointId;
        private TextView endpointName;

        EndpointHolder(View itemView) {
            super(itemView);
            endpointId = itemView.findViewById(R.id.tv_endpointId);
            endpointName = itemView.findViewById(R.id.tv_endpointName);
        }
    }

}
