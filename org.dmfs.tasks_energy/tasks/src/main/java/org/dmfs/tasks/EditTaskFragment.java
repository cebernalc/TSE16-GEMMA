/*
 * Copyright (C) 2013 Marten Gajda <marten@dmfs.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.dmfs.tasks;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import org.dmfs.provider.tasks.TaskContract;
import org.dmfs.provider.tasks.TaskContract.TaskLists;
import org.dmfs.provider.tasks.TaskContract.Tasks;
import org.dmfs.tasks.model.ContentSet;
import org.dmfs.tasks.model.Model;
import org.dmfs.tasks.model.OnContentChangeListener;
import org.dmfs.tasks.model.TaskFieldAdapters;
import org.dmfs.tasks.utils.AsyncModelLoader;
import org.dmfs.tasks.utils.ContentValueMapper;
import org.dmfs.tasks.utils.OnModelLoadedListener;
import org.dmfs.tasks.utils.TasksListCursorAdapter;
import org.dmfs.tasks.widget.TaskEdit;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * Fragment to edit task details.
 * 
 * @author Arjun Naik <arjun@arjunnaik.in>
 * @author Marten Gajda <marten@dmfs.org>
 * @author Tobias Reinsch <tobias@dmfs.org>
 */

public class EditTaskFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, OnModelLoadedListener, OnContentChangeListener,
	OnItemSelectedListener
{
	private static final String TAG = "TaskEditDetailFragment";

	public static final String PARAM_TASK_URI = "task_uri";
	public static final String PARAM_CONTENT_SET = "task_content_set";

	public static final String LIST_LOADER_URI = "uri";
	public static final String LIST_LOADER_FILTER = "filter";

	public static final String LIST_LOADER_VISIBLE_LISTS_FILTER = TaskLists.VISIBLE + "=1 and " + TaskLists.SYNC_ENABLED + "=1";

	public static final String PREFERENCE_LAST_LIST = "pref_last_list_used_for_new_event";

	/**
	 * A set of values that may affect the recurrence set of a task. If one of these values changes we have to submit all of them.
	 */
	private final static Set<String> RECURRENCE_VALUES = new HashSet<String>(Arrays.asList(new String[] { Tasks.DUE, Tasks.DTSTART, Tasks.TZ, Tasks.IS_ALLDAY,
		Tasks.RRULE, Tasks.RDATE, Tasks.EXDATE }));

	/**
	 * Projection into the task list.
	 */
	private final static String[] TASK_LIST_PROJECTION = new String[] { TaskContract.TaskListColumns._ID, TaskContract.TaskListColumns.LIST_NAME,
		TaskContract.TaskListSyncColumns.ACCOUNT_TYPE, TaskContract.TaskListSyncColumns.ACCOUNT_NAME, TaskContract.TaskListColumns.LIST_COLOR };

	/**
	 * This interface provides a convenient way to get column indices of {@link #TASK_LIST_PROJECTION} without any overhead.
	 */
	private interface TASK_LIST_PROJECTION_VALUES
	{
		public final static int id = 0;
		@SuppressWarnings("unused")
		public final static int list_name = 1;
		public final static int account_type = 2;
		@SuppressWarnings("unused")
		public final static int account_name = 3;
		public final static int list_color = 4;
	}

	private static final String KEY_VALUES = "key_values";

	private static final ContentValueMapper CONTENT_VALUE_MAPPER = new ContentValueMapper()
		.addString(Tasks.ACCOUNT_TYPE, Tasks.ACCOUNT_NAME, Tasks.TITLE, Tasks.LOCATION, Tasks.DESCRIPTION, Tasks.GEO, Tasks.URL, Tasks.TZ, Tasks.DURATION,
			Tasks.LIST_NAME)
		.addInteger(Tasks.PRIORITY, Tasks.LIST_COLOR, Tasks.TASK_COLOR, Tasks.STATUS, Tasks.CLASSIFICATION, Tasks.PERCENT_COMPLETE, Tasks.IS_ALLDAY)
		.addLong(Tasks.LIST_ID, Tasks.DTSTART, Tasks.DUE, Tasks.COMPLETED, Tasks._ID);

	private boolean mAppForEdit = true;
	private TasksListCursorAdapter mTaskListAdapter;

	private Uri mTaskUri;

	private ContentSet mValues;
	private ViewGroup mContent;
	private ViewGroup mHeader;
	private Model mModel;
	private Context mAppContext;
	private TaskEdit mEditor;
	private LinearLayout mTaskListBar;
	private boolean mSetInitialSpinnerSelection;
	private Spinner mListSpinner;


	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon screen orientation changes).
	 */
	public EditTaskFragment()
	{
	}


	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		mSetInitialSpinnerSelection = savedInstanceState == null;
	}


	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		Bundle bundle = getArguments();

		// check for supplied task information from intent
		if (bundle.containsKey(PARAM_CONTENT_SET))
		{
			mValues = bundle.getParcelable(PARAM_CONTENT_SET);
		}
		else
		{
			mTaskUri = bundle.getParcelable(PARAM_TASK_URI);
		}
		mAppContext = activity.getApplicationContext();

	}


	@SuppressWarnings("deprecation")
	@TargetApi(16)
	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		Log.v(TAG, "On create view");
		View rootView = inflater.inflate(R.layout.fragment_task_edit_detail, container, false);
		mContent = (ViewGroup) rootView.findViewById(R.id.content);
		mHeader = (ViewGroup) rootView.findViewById(R.id.header);
		mAppForEdit = !Tasks.CONTENT_URI.equals(mTaskUri) && mTaskUri != null;

		mTaskListBar = (LinearLayout) inflater.inflate(R.layout.task_list_provider_bar, mHeader);
		mListSpinner = (Spinner) mTaskListBar.findViewById(R.id.task_list_spinner);
		mTaskListAdapter = new TasksListCursorAdapter(mAppContext);
		mListSpinner.setAdapter(mTaskListAdapter);

		mListSpinner.setOnItemSelectedListener(this);

		if (android.os.Build.VERSION.SDK_INT < 11)
		{
			mListSpinner.setBackgroundDrawable(null);
		}

		if (mAppForEdit)
		{
			if (mTaskUri != null)
			{

				if (savedInstanceState == null)
				{
					mValues = new ContentSet(mTaskUri);
					mValues.addOnChangeListener(this, null, false);
					mValues.update(mAppContext, CONTENT_VALUE_MAPPER);
				}
				else
				{
					mValues = savedInstanceState.getParcelable(KEY_VALUES);
					new AsyncModelLoader(mAppContext, this).execute(mValues.getAsString(Tasks.ACCOUNT_TYPE));
					setListUri(ContentUris.withAppendedId(TaskLists.CONTENT_URI, mValues.getAsLong(Tasks.LIST_ID)), null);
				}
				// disable spinner
				mListSpinner.setEnabled(false);
				// hide spinner background
				if (android.os.Build.VERSION.SDK_INT >= 16)
				{
					mListSpinner.setBackground(null);
				}
			}
		}
		else
		{
			if (savedInstanceState == null)
			{
				// create empty ContentSet if there was no ContentSet supplied
				if (mValues == null)
				{
					mValues = new ContentSet(Tasks.CONTENT_URI);
				}
			}
			else
			{
				mValues = savedInstanceState.getParcelable(KEY_VALUES);
				new AsyncModelLoader(mAppContext, this).execute(mValues.getAsString(Tasks.ACCOUNT_TYPE));
			}
			setListUri(TaskLists.CONTENT_URI, LIST_LOADER_VISIBLE_LISTS_FILTER);
		}

		return rootView;
	}


	@Override
	public void onDestroyView()
	{
		super.onDestroyView();
		Log.v(TAG, "onDestroyView");
		if (mEditor != null)
		{
			// remove values, to ensure all listeners get released
			mEditor.setValues(null);
		}
		if (mContent != null)
		{
			mContent.removeAllViews();
		}

		final Spinner listSpinner = (Spinner) mTaskListBar.findViewById(R.id.task_list_spinner);
		listSpinner.setOnItemSelectedListener(null);
		if (mValues != null)
		{
			mValues.removeOnChangeListener(this, null);
		}
	}


	private void updateView()
	{
		/**
		 * If the model loads very slowly then this function may be called after onDetach. In this case check if Activity is <code>null</code> and return if
		 * <code>true</code>.
		 */
		Activity activity = getActivity();
		if (activity == null)
		{
			return;
		}

		final LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		if (mEditor != null)
		{
			// remove values, to ensure all listeners get released
			mEditor.setValues(null);
		}
		mContent.removeAllViews();

		mEditor = (TaskEdit) inflater.inflate(R.layout.task_edit, mContent, false);
		mEditor.setModel(mModel);
		mEditor.setValues(mValues);
		mContent.addView(mEditor);
	}


	@Override
	public void onModelLoaded(Model model)
	{
		if (model == null)
		{
			Toast.makeText(getActivity(), "Could not load Model", Toast.LENGTH_LONG).show();
			return;
		}
		mModel = model;

		updateView();
	}


	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putParcelable(KEY_VALUES, mValues);
	}


	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle)
	{
		return new CursorLoader(mAppContext, (Uri) bundle.getParcelable(LIST_LOADER_URI), TASK_LIST_PROJECTION, bundle.getString(LIST_LOADER_FILTER), null,
			null);
	}


	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
	{
		mTaskListAdapter.changeCursor(cursor);

		if (mSetInitialSpinnerSelection && cursor != null)
		{
			// set the list that was used the last time the user created an event
			SharedPreferences prefs = getActivity().getPreferences(Activity.MODE_PRIVATE);
			long lastList = prefs.getLong(PREFERENCE_LAST_LIST, -1);
			if (lastList != -1)
			{
				// iterate over all lists and select the one that matches the given id
				cursor.moveToFirst();
				while (!cursor.isAfterLast())
				{
					Long listId = cursor.getLong(TASK_LIST_PROJECTION_VALUES.id);
					if (listId != null && listId == lastList)
					{
						mListSpinner.setSelection(cursor.getPosition());
						break;
					}
					cursor.moveToNext();
				}
			}
			mSetInitialSpinnerSelection = false;
		}
	}


	@Override
	public void onLoaderReset(Loader<Cursor> loader)
	{
		mTaskListAdapter.changeCursor(null);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		final int menuId = item.getItemId();
		Activity activity = getActivity();
		if (menuId == R.id.editor_action_save)
		{
			saveAndExit();
			return true;
		}
		else if (menuId == R.id.editor_action_cancel)
		{
			activity.setResult(Activity.RESULT_CANCELED);
			activity.finish();
			return true;
		}
		return false;
	}


	@Override
	public void onContentLoaded(ContentSet contentSet)
	{
		if (contentSet.containsKey(Tasks.ACCOUNT_TYPE))
		{
			/*
			 * Don't start the model loader here, let onItemSelected do that.
			 */
			setListUri(mAppForEdit ? ContentUris.withAppendedId(TaskLists.CONTENT_URI, contentSet.getAsLong(Tasks.LIST_ID)) : TaskLists.CONTENT_URI,
				mAppForEdit ? LIST_LOADER_VISIBLE_LISTS_FILTER : null);
		}
	}


	private void setListUri(Uri uri, String filter)
	{
		if (this.isAdded())
		{
			Bundle bundle = new Bundle();
			bundle.putParcelable(LIST_LOADER_URI, uri);
			bundle.putString(LIST_LOADER_FILTER, filter);

			getLoaderManager().restartLoader(-2, bundle, this);
		}
	}


	@Override
	public void onContentChanged(ContentSet contentSet)
	{
		// nothing to do
	}


	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
	{
		Cursor c = (Cursor) arg0.getItemAtPosition(arg2);

		String accountType = c.getString(TASK_LIST_PROJECTION_VALUES.account_type);
		int listColor = c.getInt(TASK_LIST_PROJECTION_VALUES.list_color);
		mTaskListBar.setBackgroundColor(listColor);

		if (!mAppForEdit)
		{
			long listId = c.getLong(TASK_LIST_PROJECTION_VALUES.id);
			mValues.put(Tasks.LIST_ID, listId);
		}

		if (mModel == null || !mModel.getAccountType().equals(accountType))
		{
			// the model changed, load the new model
			new AsyncModelLoader(mAppContext, EditTaskFragment.this).execute(accountType);
		}
	}


	@Override
	public void onNothingSelected(AdapterView<?> arg0)
	{
		// nothing to do here
	}


	/**
	 * Persist the current task (if anything has been edited) and close the editor.
	 */
	public void saveAndExit()
	{
		// TODO: put that in a background task
		Activity activity = getActivity();

		int resultCode = Activity.RESULT_CANCELED;
		Intent result = null;
		int toastId = -1;

		mEditor.updateValues();

		if (mValues.isInsert() || mValues.isUpdate())
		{
			if (!TextUtils.isEmpty(TaskFieldAdapters.TITLE.get(mValues)) || mValues.isUpdate())
			{

				if (mValues.updatesAnyKey(RECURRENCE_VALUES))
				{
					mValues.ensureUpdates(RECURRENCE_VALUES);
				}

				mTaskUri = mValues.persist(activity);

				// return proper result
				result = new Intent();
				result.setData(mTaskUri);
				resultCode = Activity.RESULT_OK;
				toastId = R.string.activity_edit_task_task_saved;
			}
			else
			{
				toastId = R.string.activity_edit_task_empty_task_not_saved;
			}
		}
		else
		{
			Log.i(TAG, "nothing to save");
		}

		if (toastId != -1)
		{
			Toast.makeText(activity, toastId, Toast.LENGTH_SHORT).show();
		}

		if (result != null)
		{
			activity.setResult(resultCode, result);
		}
		else
		{
			activity.setResult(resultCode);
		}

		if (!mAppForEdit)
		{
			// store last list used
			SharedPreferences prefs = getActivity().getPreferences(Activity.MODE_PRIVATE);
			Editor editor = prefs.edit();
			editor.putLong(PREFERENCE_LAST_LIST, mListSpinner.getSelectedItemId());
			editor.commit();
		}

		activity.finish();
	}
}
