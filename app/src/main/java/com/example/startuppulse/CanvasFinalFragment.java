package com.example.startuppulse;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.startuppulse.data.Ideia;

public class CanvasFinalFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_canvas_final, container, false);
    }
    public static CanvasFinalFragment newInstance(Ideia ideia, boolean isReadOnly) {
        CanvasFinalFragment fragment = new CanvasFinalFragment();
        Bundle args = new Bundle();
        args.putSerializable("ideia", ideia);
        fragment.setArguments(args);
        return fragment;
    }
}