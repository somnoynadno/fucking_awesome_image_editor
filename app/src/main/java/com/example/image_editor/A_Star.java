package com.example.image_editor;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.view.View.OnTouchListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.PriorityQueue;

import static java.lang.Math.abs;

public class A_Star extends Conductor implements OnTouchListener {

    private Integer typeDraw = 0;
    private Bitmap bitmap;
    private ArrayList<Pixel> remstart, remfinish;
    private boolean start = false, finish = false;
    private Button change_start;
    private Button change_end;
    private Point pnt_start, pnt_finish;
    private Point[][] par;

    private ArrayList<Button> buttons;
    private ImageView imageView;

    A_Star(ArrayList<Button> buttons, ImageView imageView) {
        /* buttons - ArrayList<Button>
        *  buttons[0] - set start Button
        *  buttons[1] - set finish Button
        *  buttons[2] - set wall Button
        *  buttons[3] - do algo
        * */
        super(buttons.get(3));
        this.change_start = buttons.get(0);
        this.change_end = buttons.get(1);
        this.imageView = imageView;
        this.buttons = buttons;
        remstart = new ArrayList<>();
        remfinish = new ArrayList<>();

    }

    private void ConfigWallButton(Button button) {
        button.setText("set wall");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnsetwall(v);
            }
        });
    }

    private void ConfigFinishButton(Button button) {
        button.setText("Set finish");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnsetto(v);
            }
        });
    }

    private void ConfigStartButton(Button button) {
        button.setText("Set start");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnsetfrom(v);
            }
        });
    }

    void touchToolbar() {
        super.touchToolbar();
        buttons.get(3).setText("Do algo");
        ConfigWallButton(buttons.get(2));
        ConfigFinishButton(buttons.get(1));
        ConfigStartButton(buttons.get(0));

        bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        imageView.setImageBitmap(bitmap);
        imageView.setOnTouchListener(this);
    }

    public void btnsetfrom(View view) {
        typeDraw = 1;
        if (start) {
            for (int i = 0; i < remstart.size(); i++) {
                bitmap.setPixel(remstart.get(i).getX(),
                        remstart.get(i).getY(),
                        remstart.get(i).getColor());
            }
            remstart.clear();
            change_start.setText("set from");
            start = false;
        }
    }

    public void btnsetto(View view) {
        typeDraw = 2;
        if (finish) {
            for (int i = 0; i < remfinish.size(); i++) {
                bitmap.setPixel(remfinish.get(i).getX(),
                        remfinish.get(i).getY(),
                        remfinish.get(i).getColor());
            }
            remfinish.clear();
            change_end.setText("set to");
            finish = false;
        }
    }

    public void btnsetwall(View view) {
        typeDraw = 3;
    }

    private boolean canPutRect(int rad, int mx, int my) {
        for (int i = -rad; i <= rad; i++) {
            for (int j = -rad; j <= rad; j++) {
                if (0 > mx + i || mx + i >= bitmap.getWidth() ||
                        0 > my + j || my + j >= bitmap.getHeight()) {
                    continue;
                }
                if (abs(i) + abs(j) <= rad) {
                    if (bitmap.getPixel(mx + i, my + j) == Color.rgb(10, 255, 10) ||
                            bitmap.getPixel(mx + i, my + j) == Color.rgb(255, 10, 10)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void errorTouched() {
        // todo: hand this
        return;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int mx = (int) event.getX();
        int my = (int) event.getY();
        Log.i("upd", ((Integer)(mx)).toString() + " " + ((Integer)(my)).toString());
//        Log.i("UPD", "touch");
        if (typeDraw == 3) {
            int rad = 15;
            if (!canPutRect(rad, mx, my)) {
                errorTouched();
                return false;
            }
            for (int i = -rad; i <= rad; i++) {
                for (int j = -rad; j <= rad; j++) {
                    if (0 > mx + i || mx + i >= bitmap.getWidth()) {
                        continue;
                    }
                    if (0 > my + j || my + j >= bitmap.getHeight()) {
                        continue;
                    }
                    if (abs(i) + abs(j) <= rad) {
                        Pixel now = new Pixel(mx + i, my + j, bitmap.getPixel(mx + i, my + j));
                        remfinish.add(now);
                        bitmap.setPixel(mx + i, my + j, Color.WHITE);
                    }
                }
            }
            imageView.invalidate();
            return true;
        } else if (typeDraw == 2) {
            int rad = 30;
            if (finish) return false;
            if (!canPutRect(rad, mx, my)) {
                errorTouched();
                return false;
            }
            for (int i = -rad; i <= rad; i++) {
                for (int j = -rad; j <= rad; j++) {
                    if (0 > mx + i || mx + i >= bitmap.getWidth()) {
                        continue;
                    }
                    if (0 > my + j || my + j >= bitmap.getHeight()) {
                        continue;
                    }
                    if (abs(i) + abs(j) <= rad) {
                        Pixel now = new Pixel(mx + i, my + j, bitmap.getPixel(mx + i, my + j));
                        remfinish.add(now);
                        bitmap.setPixel(mx + i, my + j, Color.rgb(255, 10, 10));
                    }
                }
            }
            pnt_finish = new Point(mx, my);
            finish = true;
            imageView.invalidate();
            change_end.setText("delete to");
            return true;
        } else if (typeDraw == 1) {
            int rad = 30;
            if (start) return false;
            if (!canPutRect(rad, mx, my)) {
                errorTouched();
                return false;
            }
            for (int i = -rad; i <= rad; i++) {
                for (int j = -rad; j <= rad; j++) {
                    if (0 > mx + i || mx + i >= bitmap.getWidth()) {
                        continue;
                    }
                    if (0 > my + j || my + j >= bitmap.getHeight()) {
                        continue;
                    }
                    if (abs(i) + abs(j) <= rad) {
                        Pixel now = new Pixel(mx + i, my + j, bitmap.getPixel(mx + i, my + j));
                        remstart.add(now);
                        bitmap.setPixel(mx + i, my + j, Color.rgb(10, 255, 10));
                    }
                }
            }
            pnt_start = new Point(mx, my);
            start = true;
            imageView.invalidate();
            change_start.setText("delete from");
            return true;
        } else {
            // TODO: toast with message: wtf u don't click button
        }
        return false;
    }

    private boolean check() {
        // todo: hand this
        return true;
    }

    private boolean cor(Point a) {
        return bitmap.getPixel(a.x, a.y) != Color.WHITE;
    }

    private ArrayList<Point> reconstruct_path() {
        ArrayList<Point> res = new ArrayList<Point>();
        Point now = pnt_finish;
        while (now != pnt_start){
            res.add(now);
            now = par[now.x][now.y];
        }
        res.add(pnt_start);
        Collections.reverse(res);
        return res;
    }

    private ArrayList<Point> A_Star(int n, int m) {

        boolean[][] in_open = new boolean[n][m];
        par = new Point[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                in_open[i][j] = false;
            }
        }
//        Log.i("UPD", "A_Star: end for in for");
        int[] dirx = {0, 0, 1, -1};
        int[] diry = {1, -1, 0, 0};

        HashSet<Point> closedset = new HashSet<Point>();
        PointComparator myComp = new PointComparator(pnt_finish, n, m);
        PriorityQueue<Point> openset =
                new PriorityQueue<Point>(10, myComp);
        openset.add(pnt_start);
        myComp.setInG(pnt_start, 0);
        int k=0;
        Log.i("UPD", ((Integer)pnt_start.x).toString() + " " + ((Integer)pnt_start.y).toString());
        Log.i("UPD", ((Integer)pnt_finish.x).toString() + " " + ((Integer)pnt_finish.y).toString());
        while (!openset.isEmpty()) {
            Point x = openset.poll();

//            bitmap.setPixel(x.x, x.y, Color.BLUE);
//            imageView.invalidate();

            if (x.x == pnt_finish.x && x.y == pnt_finish.y) {
                return reconstruct_path();
            }
            closedset.add(x);
            for (int i = 0; i < 4; i++) {
                boolean tentative_is_better = false;
                Point y = new Point(dirx[i] + x.x, diry[i] + x.y);
                if (y.x < 0 || y.x >= n) continue;
                if (y.y < 0 || y.y >= m) continue;
                if (!cor(y)) continue;
                if (closedset.contains(y)) continue;
                int tentative_g_score = myComp.getInG(x) + 1;
                if (!in_open[y.x][y.y]) {
                    tentative_is_better = true;
                    in_open[y.x][y.y] = true;
                    openset.add(y);
                } else if (tentative_g_score < myComp.getInG(y)) {
                    tentative_is_better = true;
                }
                if (tentative_is_better) {
                    par[y.x][y.y] = x;
                    myComp.setInG(y, tentative_g_score);
                }
            }
        }
        return new ArrayList<>();
    }

    private void algorithm() {

        if (!check()) {
            return;
        }
        int n = bitmap.getWidth();
        int m = bitmap.getHeight();

        ArrayList<Point> answer = A_Star(n, m);

        for(int i=0;i<answer.size();i++){
            bitmap.setPixel(answer.get(i).x,
                    answer.get(i).y,
                    Color.YELLOW);
        }
        Log.i("UPD", "END");

    }

    void touchRun(){
        super.touchRun();
        algorithm();
        imageView.invalidate();
    }

}