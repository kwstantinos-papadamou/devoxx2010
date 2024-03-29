/*
 * Copyright 2010 Peter Kuterna
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.peterkuterna.android.apps.devoxxsched.ui;

import static net.peterkuterna.android.apps.devoxxsched.util.UIUtils.buildStyledSnippet;
import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Speakers;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.PinnedHeaderListView;
import net.peterkuterna.android.apps.devoxxsched.util.NotifyingAsyncQueryHandler;
import net.peterkuterna.android.apps.devoxxsched.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.Spannable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AlphabetIndexer;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

/**
 * {@link ListActivity} to list the {@link Speakers}
 */
public class SpeakersActivity extends ListActivity implements AsyncQueryListener {

    private CursorAdapter mAdapter;

    private NotifyingAsyncQueryHandler mHandler;

    private int mPinnedHeaderBackgroundColor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        if (!getIntent().hasCategory(Intent.CATEGORY_TAB)) {
            setContentView(R.layout.activity_speakers);

            final String customTitle = getIntent().getStringExtra(Intent.EXTRA_TITLE);
            ((TextView) findViewById(R.id.title_text)).setText(
                    customTitle != null ? customTitle : getTitle());
        } else {
            setContentView(R.layout.activity_speakers_content);
        }

        final Uri speakersUri = getIntent().getData();

        String[] projection;
        if (!Speakers.isSearchUri(speakersUri)) {
            mAdapter = new SpeakersAdapter(this);
            projection = SpeakersQuery.PROJECTION;
            getListView().setFastScrollEnabled(true);
        } else {
            mAdapter = new SearchAdapter(this);
            projection = SearchQuery.PROJECTION;
            getListView().setFastScrollEnabled(false);
        }

        setListAdapter(mAdapter);

        setupListView(getIntent());

        mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
        mHandler.startQuery(speakersUri, projection, Speakers.DEFAULT_SORT);
	}

	private void setupListView(Intent intent) {
    	if (mAdapter instanceof SpeakersAdapter) {
        	final PinnedHeaderListView list = (PinnedHeaderListView) getListView();
			mPinnedHeaderBackgroundColor = getResources().getColor(R.color.header_background);
			View pinnedHeader = getLayoutInflater().inflate(R.layout.list_item_header, list, false);
			list.setPinnedHeaderView(pinnedHeader);
			list.setDividerHeight(0);
			list.setOnScrollListener((SpeakersAdapter) mAdapter);
    	}
    }

    /** {@inheritDoc} */
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        startManagingCursor(cursor);
        mAdapter.changeCursor(cursor);
    }

    /** {@inheritDoc} */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final Cursor cursor = (Cursor) mAdapter.getItem(position);
        final String speakerId = cursor.getString(SpeakersQuery.SPEAKER_ID);
        final Uri speakerUri = Speakers.buildSpeakerUri(speakerId);
        startActivity(new Intent(Intent.ACTION_VIEW, speakerUri));
    }

    /** Handle "home" title-bar action. */
    public void onHomeClick(View v) {
        UIUtils.goHome(this);
    }

    /** Handle "search" title-bar action. */
    public void onSearchClick(View v) {
        UIUtils.goSearch(this);
    }

    public static final class SpeakerItemViews {
        View headerView;
        TextView headerTextView;
        View dividerView;
        TextView nameView;
        TextView companyView;
        CheckBox starButton;
    }
    
    final static class PinnedHeaderCache {
        public TextView titleView;
        public ColorStateList textColor;
        public Drawable background;
    }
    
    private abstract class BaseAdapter extends CursorAdapter {

		public BaseAdapter(Context context) {
			super(context, null, true);
		}
    	
	    protected void findAndCacheViews(View view) {
	        // Get the views to bind to
	        SpeakerItemViews views = new SpeakerItemViews();
	        views.headerView = view.findViewById(R.id.header);
	        views.headerTextView = (TextView) view.findViewById(R.id.header_text);
	        views.dividerView = view.findViewById(R.id.session_divider);
	        views.nameView = (TextView) view.findViewById(R.id.speaker_name);
	        views.companyView = (TextView) view.findViewById(R.id.speaker_company);
	        views.starButton = (CheckBox) view.findViewById(R.id.star_button);
	        view.setTag(views);
	    }
	    
    }
    
    /**
     * {@link CursorAdapter} that renders a {@link SpeakersQuery}.
     */
    private class SpeakersAdapter extends BaseAdapter 
    	implements SectionIndexer, OnScrollListener, PinnedHeaderListView.PinnedHeaderAdapter {
    	
    	private AlphabetIndexer mIndexer;
    	private boolean mDisplaySectionHeaders = true;
	
		private final Context mContext;

        public SpeakersAdapter(Context context) {
            super(context);
            
            this.mContext = context;
        }

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
		    if (!getCursor().moveToPosition(position)) {
		        throw new IllegalStateException("couldn't move cursor to position " + position);
		    }
		
		    boolean newView;
		    View v;
		    if (convertView == null || convertView.getTag() == null) {
		        newView = true;
		        v = newView(mContext, getCursor(), parent);
		    } else {
		        newView = false;
		        v = convertView;
		    }
		    bindView(v, mContext, getCursor());
		    bindSectionHeader(v, position, mDisplaySectionHeaders);
		    return v;
		}
		
        /** {@inheritDoc} */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = getLayoutInflater().inflate(R.layout.list_item_speaker, parent, false);
			findAndCacheViews(v);
			return v;
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
        	SpeakerItemViews views = (SpeakerItemViews) view.getTag();
            views.nameView.setText(cursor.getString(SpeakersQuery.LAST_NAME) + " " + cursor.getString(SpeakersQuery.FIRST_NAME));
            views.companyView.setText(cursor.getString(SpeakersQuery.COMPANY));
            final boolean starred = cursor.getInt(SpeakersQuery.CONTAINS_STARRED) != 0;
            views.starButton.setVisibility(starred ? View.VISIBLE : View.INVISIBLE);
            views.starButton.setChecked(starred);
        }

		private void bindSectionHeader(View view, int position, boolean displaySectionHeaders) {
			SpeakerItemViews views = (SpeakerItemViews) view.getTag();
		    if (!displaySectionHeaders) {
		    	views.headerView.setVisibility(View.GONE);
		    	views.dividerView.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
		    } else {
		        final int section = getSectionForPosition(position);
		        if (getPositionForSection(section) == position) {
		            String title = (String) mIndexer.getSections()[section];
		            views.headerTextView.setText(title);
			    	views.headerView.setVisibility(View.VISIBLE);
			    	views.dividerView.setVisibility(View.GONE);
		        } else {
		        	views.headerView.setVisibility(View.GONE);
			    	views.dividerView.setVisibility(View.VISIBLE);
		        }
		
		        // move the divider for the last item in a section
		        if (getPositionForSection(section + 1) - 1 == position) {
			    	views.dividerView.setVisibility(View.GONE);
		        } else {
			    	views.dividerView.setVisibility(View.VISIBLE);
		        }
		    }
		}
	
		@Override
		public void changeCursor(Cursor cursor) {
			super.changeCursor(cursor);
			
			updateIndexer(cursor);
		}
		
		private void updateIndexer(Cursor cursor) {
			if (cursor == null) {
				mIndexer = null;
				return;
			}

			mIndexer = new AlphabetIndexer(cursor, SpeakersQuery.LAST_NAME, "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
		}

		@Override
		public int getPositionForSection(int section) {
			final Cursor cursor = getCursor();
			if (cursor == null || cursor.isClosed()) {
				return -1;
			}
			
			return mIndexer.getPositionForSection(section);
		}

		@Override
		public int getSectionForPosition(int position) {
			final Cursor cursor = getCursor();
			if (cursor == null || cursor.isClosed()) {
				return -1;
			}
			
			return mIndexer.getSectionForPosition(position);
		}

		@Override
		public Object[] getSections() {
			final Cursor cursor = getCursor();
			if (cursor == null || cursor.isClosed()) {
		        return new String[] { " " };
			}
			
			return mIndexer.getSections();
		}
          
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
		        int totalItemCount) {
		    if (view instanceof PinnedHeaderListView) {
		        ((PinnedHeaderListView) view).configureHeaderView(firstVisibleItem);
		    }
		}
		
		public void onScrollStateChanged(AbsListView view, int scrollState) {
		}
	
		/**
		 * Computes the state of the pinned header.  It can be invisible, fully
		 * visible or partially pushed up out of the view.
		 */
		public int getPinnedHeaderState(int position) {
			final Cursor cursor = getCursor();
		    if (mIndexer == null || cursor == null || cursor.getCount() == 0) {
		        return PINNED_HEADER_GONE;
		    }
		
		    if (position < 0) {
		        return PINNED_HEADER_GONE;
		    }
		
		    // The header should get pushed up if the top item shown
		    // is the last item in a section for a particular letter.
		    int section = getSectionForPosition(position);
		    int nextSectionPosition = getPositionForSection(section + 1);
		    if (nextSectionPosition != -1 && position == nextSectionPosition - 1) {
		        return PINNED_HEADER_PUSHED_UP;
		    }
		
		    return PINNED_HEADER_VISIBLE;
		}
	
		/**
		 * Configures the pinned header by setting the appropriate text label
		 * and also adjusting color if necessary.  The color needs to be
		 * adjusted when the pinned header is being pushed up from the view.
		 */
		public void configurePinnedHeader(View header, int position, int alpha) {
		    PinnedHeaderCache cache = (PinnedHeaderCache) header.getTag();
	
		    if (cache == null) {
		        cache = new PinnedHeaderCache();
		        cache.titleView = (TextView) header.findViewById(R.id.header_text);
		        cache.textColor = cache.titleView.getTextColors();
		        cache.background = header.getBackground();
		        header.setTag(cache);
		    }
		
		    int section = getSectionForPosition(position);
		    
		    if (section != -1) {
			    String title = (String) mIndexer.getSections()[section];
			    cache.titleView.setText(title);
			
			    if (alpha == 255) {
			        // Opaque: use the default background, and the original text color
			        header.setBackgroundDrawable(cache.background);
			        cache.titleView.setTextColor(cache.textColor);
			    } else {
			        // Faded: use a solid color approximation of the background, and
			        // a translucent text color
			    	final int diffAlpha = 255 - alpha;
			    	final int red = Color.red(mPinnedHeaderBackgroundColor);
			    	final int diffRed = 255 - red;
			    	final int green = Color.green(mPinnedHeaderBackgroundColor);
			    	final int diffGreen = 255 - green;
			    	final int blue = Color.blue(mPinnedHeaderBackgroundColor);
			    	final int diffBlue = 255 - blue;
			        header.setBackgroundColor(Color.rgb(
			        		red + (diffRed * diffAlpha / 255),
			        		green + (diffGreen * diffAlpha / 255),
			        		blue + (diffBlue * diffAlpha / 255)));
			
			        int textColor = cache.textColor.getDefaultColor();
			        cache.titleView.setTextColor(Color.argb(alpha,
			                Color.red(textColor), Color.green(textColor), Color.blue(textColor)));
			    }
		    }
		}
    }

    /**
     * {@link CursorAdapter} that renders a {@link SearchQuery}.
     */
    private class SearchAdapter extends BaseAdapter implements PinnedHeaderListView.PinnedHeaderAdapter {
        public SearchAdapter(Context context) {
            super(context);
        }

        @Override
		public int getPinnedHeaderState(int position) {
			return 0;
		}

		@Override
		public void configurePinnedHeader(View header, int position, int alpha) {
		}

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = getLayoutInflater().inflate(R.layout.list_item_speaker, parent, false);
			findAndCacheViews(v);
			return v;
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
        	SpeakerItemViews views = (SpeakerItemViews) view.getTag();
            final String name = cursor.getString(SearchQuery.LAST_NAME) + " " + cursor.getString(SearchQuery.FIRST_NAME);
            views.nameView.setText(name);
            final String snippet = cursor.getString(SearchQuery.SEARCH_SNIPPET);
            final Spannable styledSnippet = buildStyledSnippet(snippet);
            views.companyView.setText(styledSnippet);
            final boolean starred = cursor.getInt(SearchQuery.CONTAINS_STARRED) != 0;
            views.starButton.setVisibility(starred ? View.VISIBLE : View.INVISIBLE);
            views.starButton.setChecked(starred);
            views.dividerView.setVisibility(View.GONE);
            views.headerView.setVisibility(View.GONE);
        }
    
    }

    /** {@link Speakers} query parameters. */
    private interface SpeakersQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                Speakers.SPEAKER_ID,
                Speakers.FIRST_NAME,
                Speakers.LAST_NAME,
                Speakers.COMPANY,
                Speakers.CONTAINS_STARRED,
        };

        int _ID = 0;
        int SPEAKER_ID = 1;
        int FIRST_NAME = 2;
        int LAST_NAME = 3;
        int COMPANY = 4;
        int CONTAINS_STARRED = 5;
    }

    /** {@link Speakers} search query parameters. */
    private interface SearchQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                Speakers.SPEAKER_ID,
                Speakers.FIRST_NAME,
                Speakers.LAST_NAME,
                Speakers.SEARCH_SNIPPET,
                Speakers.CONTAINS_STARRED,
        };

        int _ID = 0;
        int SPEAKER_ID = 1;
        int FIRST_NAME = 2;
        int LAST_NAME = 3;
        int SEARCH_SNIPPET = 4;
        int CONTAINS_STARRED = 5;
    }

}
