package nb.cblink.vnnews.modelview;

import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.ScaleAnimation;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.yalantis.phoenix.PullToRefreshView;

import org.zakariya.stickyheaders.SectioningAdapter;
import org.zakariya.stickyheaders.StickyHeaderLayoutManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import nb.cblink.vnnews.R;
import nb.cblink.vnnews.data.DataFactory;
import nb.cblink.vnnews.model.FeedTopic;
import nb.cblink.vnnews.model.News;
import nb.cblink.vnnews.view.activity.MainActivity;

/**
 * Created by nguyenbinh on 26/10/2016.
 */

public class NewsFeedModelView {
    private MainActivity context;
    private RecyclerView recyclerView;
    private PullToRefreshView mPullToRefreshView;
    private static final String TAG = NewsFeedModelView.class.getSimpleName();
    private Window window;
    private DatabaseReference mFirebaseDatabaseReference;
    private NewsFeedAdapter adapter;

    public NewsFeedModelView(MainActivity context, RecyclerView recyclerView, PullToRefreshView mPullToRefreshView) {
        this.context = context;
        this.recyclerView = recyclerView;
        this.mPullToRefreshView = mPullToRefreshView;
        window = context.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        init();
        getData();
    }

    private void getData() {
        if (!DataFactory.getInstance().haveData()) {
            long currentMilisecond;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", new Locale("vi"));
            for (final FeedTopic topic : DataFactory.getInstance().data) {
                currentMilisecond = System.currentTimeMillis();
                String currentDateandTime = sdf.format(currentMilisecond);
                for (String refer : topic.getListReference()) {
                    mFirebaseDatabaseReference.child("normal/" + refer + "/" + currentDateandTime).addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                            News news = new News();
                            HashMap mapData = (HashMap) dataSnapshot.getValue();
                            news.setNewsTitle((String) mapData.get("title"));
                            news.setNewsUrl((String) mapData.get("url"));
                            news.setImageUrl((String) mapData.get("image_url"));
                            news.setTime((String) mapData.get("date"));
                            news.setPaperName(((String) mapData.get("labels")));
                            news.setContent(((String) mapData.get("content")).replaceAll("\n", "").replaceAll("\t", ""));
                            topic.addNews(news);
                            adapter.notifySectionDataSetChanged(DataFactory.getInstance().data.indexOf(topic));
                        }

                        @Override
                        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                            News news = new News();
                            HashMap mapData = (HashMap) dataSnapshot.getValue();
                            news.setNewsTitle((String) mapData.get("title"));
                            news.setNewsUrl((String) mapData.get("url"));
                            news.setImageUrl((String) mapData.get("image_url"));
                            news.setTime((String) mapData.get("date"));
                            news.setPaperName(((String) mapData.get("labels")));
                            news.setContent(((String) mapData.get("content")).replaceAll("\n", "").replaceAll("\t", ""));
                            topic.addNews(news);
                            adapter.notifySectionDataSetChanged(DataFactory.getInstance().data.indexOf(topic));
                        }

                        @Override
                        public void onChildRemoved(DataSnapshot dataSnapshot) {

                        }

                        @Override
                        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
                if (topic.getListNews().size() < 10) {
                    currentMilisecond -= (24 * 60 * 60 * 1000);
                    currentDateandTime = sdf.format((new Date(currentMilisecond)));
                    //Lay them tin
                    for (String refer : topic.getListReference()) {
                        mFirebaseDatabaseReference.child("normal/" + refer + "/" + currentDateandTime).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot.getValue() != null) {
                                    Iterator iterator = ((HashMap) dataSnapshot.getValue()).values().iterator();
                                    while (iterator.hasNext()) {
                                        News news = new News();
                                        HashMap mapData = (HashMap) iterator.next();
                                        news.setNewsTitle((String) mapData.get("title"));
                                        news.setNewsUrl((String) mapData.get("url"));
                                        news.setImageUrl((String) mapData.get("image_url"));
                                        news.setTime((String) mapData.get("date"));
                                        news.setPaperName(((String) mapData.get("labels")));
                                        news.setContent(((String) mapData.get("content")).replaceAll("\n", "").replaceAll("\t", ""));
                                        topic.addNews(news);
                                        adapter.notifySectionDataSetChanged(DataFactory.getInstance().data.indexOf(topic));
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                }
            }
        }
    }

    void init() {
        StickyHeaderLayoutManager layoutManager = new StickyHeaderLayoutManager();
        recyclerView.setLayoutManager(layoutManager);
        layoutManager.setHeaderPositionChangedCallback(new StickyHeaderLayoutManager.HeaderPositionChangedCallback() {
            @Override
            public void onHeaderPositionChanged(int sectionIndex, View header, StickyHeaderLayoutManager.HeaderPosition oldPosition, StickyHeaderLayoutManager.HeaderPosition newPosition) {
                Log.i(TAG, "onHeaderPositionChanged: section: " + sectionIndex + " -> old: " + oldPosition.name() + " new: " + newPosition.name());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    boolean elevated = newPosition == StickyHeaderLayoutManager.HeaderPosition.STICKY;
                    header.setElevation(elevated ? 8 : 0);
                }
                if (sectionIndex == 0 && newPosition.name().equals("NATURAL")) {
                    mPullToRefreshView.setEnabled(true);
//                    if (MainActivity.toolbar.getVisibility() != View.VISIBLE)
//                        MainActivity.toolbar.setVisibility(View.VISIBLE);
                } else {
                    mPullToRefreshView.setEnabled(false);
//                    if (MainActivity.toolbar.getVisibility() != View.GONE) {
//                        if (sectionIndex != 0) {
//                            MainActivity.toolbar.setVisibility(View.GONE);
//                        }
//                    }
                }

                if ((oldPosition.name().equals("NATURAL") && newPosition.name().equals("STICKY")) || oldPosition.name().equals("TRAILING") && newPosition.name().equals("STICKY")) {
                    ScaleAnimation scale = new ScaleAnimation((float) 1.0, (float) 0.25, (float) 1.0, (float) 0.25);
                    scale.setFillAfter(true);
                    scale.setDuration(600);
                    ((FeedTopic) header.getTag()).setSizeText(55);
                    ((FeedTopic) header.getTag()).setBackgroundSrcVisibility(View.INVISIBLE);
                    header.startAnimation(scale);
                    //window.setStatusBarColor(((FeedTopic)header.getTag()).getColorTopic() - 0xff00000f);
                } else if (oldPosition.name().equals("STICKY") && newPosition.name().equals("NATURAL")) {
                    ScaleAnimation scale = new ScaleAnimation((float) 1.0, (float) 1.0, (float) 1.0, (float) 1.0);
                    scale.setFillAfter(true);
                    scale.setDuration(0);
                    ((FeedTopic) header.getTag()).setSizeText(25);
                    ((FeedTopic) header.getTag()).setBackgroundSrcVisibility(View.VISIBLE);
                    header.startAnimation(scale);
                }
//                if (MainActivity.toolbar.getVisibility() == View.VISIBLE) {
//                    window.setStatusBarColor(ContextCompat.getColor(context, R.color.colorPrimaryDark));
//                }
            }
        });

        adapter = new NewsFeedAdapter(context, window, mFirebaseDatabaseReference);
        recyclerView.setAdapter(adapter);
        mPullToRefreshView.setOnRefreshListener(new PullToRefreshView.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mPullToRefreshView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mPullToRefreshView.setRefreshing(false);
                        adapter.notifyDataSetChanged();
                        recyclerView.invalidate();

                    }
                }, 1000);
            }
        });
    }
}
