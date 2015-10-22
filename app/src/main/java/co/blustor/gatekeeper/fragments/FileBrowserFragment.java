package co.blustor.gatekeeper.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import co.blustor.gatekeeper.R;
import co.blustor.gatekeeper.data.AsyncFilestore;
import co.blustor.gatekeeper.data.FTPAsyncFilestore;
import co.blustor.gatekeeper.data.File;
import co.blustor.gatekeeper.ui.FileBrowserView;

public class FileBrowserFragment extends Fragment implements AsyncFilestore.Listener {
    private FileBrowserView mFileGrid;

    private FTPAsyncFilestore ftpAsyncFilestore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file_browser, container, false);
        initializeViews(view);
        initializeData();
        return view;
    }

    @Override
    public void onDestroyView() {
        uninitializeClient();
        super.onDestroyView();
    }

    private void initializeViews(View view) {
        mFileGrid = (FileBrowserView) view.findViewById(R.id.file_browser);
    }

    private void initializeData() {
        ftpAsyncFilestore = new FTPAsyncFilestore();
        ftpAsyncFilestore.listFiles(this);
    }

    private void uninitializeClient() {
        ftpAsyncFilestore.finish();
    }

    @Override
    public void onListFiles(final List<File> files) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFileGrid.setAdapter(new FileBrowserView.Adapter(getActivity(), files));
            }
        });
    }
}