package com.example.rubusvideofps;

import android.util.Log;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

public class UpdateFPS {
    class Positions {
        public HashSet<Integer> positions;
        public long firstPosTimeMs;
        public long lastPosTimeMs;
        public int lastPosValue;
        public Positions() {
            positions = new HashSet<Integer>();
            firstPosTimeMs = 0;
            lastPosValue = -1;
        }

        // false: -1, or < last pos value
        // true: 0 or >= last pos value
        public boolean UpdatePos(int pos) {
            if (pos == -1) return false;
            if (pos < lastPosValue) return false;
            if (positions.size() == 0) firstPosTimeMs = System.currentTimeMillis();
            lastPosValue = pos;
            lastPosTimeMs = System.currentTimeMillis();
            positions.add(pos);
            return true;
        }
    }

    private String TAG = "UpdateFPS";
    private long mLastCalculateFPSTime = 0;

    private TextView mFPS;
    private Vector<Positions> mPosNodes;

    public void Init(TextView fpsView) {
        mFPS = fpsView;
        mPosNodes = new Vector<>();
    }

    private void calculateFPS() {
        long ms = System.currentTimeMillis();
        if (ms - mLastCalculateFPSTime < 500) return;

        // remove all out date nodes
        int max = mPosNodes.size() - 1;
        for (int i = max; i >= 0; --i) {
            Positions node = mPosNodes.elementAt(i);
            if (ms - node.firstPosTimeMs > 3000)
                mPosNodes.remove(i);
        }

        // count all frames and cost
        int frames = 0;
        long cost = 0;
        max = mPosNodes.size();
        for (int i = 0; i < max; ++i) {
            Positions node = mPosNodes.elementAt(i);
            if (node.positions.size() > 1) {
                frames += (node.positions.size() - 1);
                cost += (node.lastPosTimeMs - node.firstPosTimeMs);
            }
        }

        float costSeconds = cost > 0 ? cost / 1000.0f : 1.0f;
        float fps = frames / costSeconds;
        String s = String.format("%5.2f", fps);
        mFPS.setText(s);
        mLastCalculateFPSTime = ms;
    }

    private Positions checkLastNode(int pos) {
        if (mPosNodes.size() == 0) {
            Positions node = new Positions();
            mPosNodes.add(node);
            return mPosNodes.lastElement();
        }

        long ms = System.currentTimeMillis();
        Positions node = mPosNodes.lastElement();
        if (node.positions.size() > 0 &&
                (ms - node.firstPosTimeMs > 3000 ||
                 pos < node.lastPosValue)) {
            Positions newNode = new Positions();
            mPosNodes.add(newNode);
            return mPosNodes.lastElement();
        }
        return mPosNodes.lastElement();
    }

    public void UpdatePos(int pos) {
        if (pos == -1) {
            calculateFPS();
            return;
        }

        // long ms = System.currentTimeMillis();
        // Log.e(TAG, String.format("update pos: %d, at time: %d", pos, ms));

        Positions node = checkLastNode(pos);
        // if (node.positions.size() > 1) {
        //    float cost = (ms - node.lastPosTimeMs) / 1000.0f;
        //    float maxFPS = 1 / cost;
        //    Log.i(TAG, String.format("max fps: %5.2f", maxFPS));
        // }
        if (!node.UpdatePos(pos)) {
            Log.e(TAG, "ERROR, update pos failed.");
        }

        calculateFPS();
    }
}
