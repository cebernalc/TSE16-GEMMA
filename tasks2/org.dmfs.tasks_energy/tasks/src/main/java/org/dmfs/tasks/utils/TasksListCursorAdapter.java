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

package org.dmfs.tasks.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import org.dmfs.provider.tasks.TaskContract;
import org.dmfs.tasks.R;
import org.dmfs.tasks.widget.AbstractFieldView;


/**
 * An adapter to adapt a cursor containing task lists to a {@link Spinner}.
 * 
 * @author Arjun Naik<arjun@arjunnaik.in>
 */
public class TasksListCursorAdapter extends android.support.v4.widget.CursorAdapter implements SpinnerAdapter
{
	LayoutInflater mInflater;

	private int mTaskColorColumn;
	private int mTaskNameColumn;
	private int mAccountNameColumn;


	public TasksListCursorAdapter(Context context)
	{
		super(context, null, 0 /* don't register a content observer to avoid a context leak! */);
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}


	@Override
	public void changeCursor(Cursor c)
	{
		super.changeCursor(c);
		if (c != null)
		{
			mTaskColorColumn = c.getColumnIndex(TaskContract.TaskListColumns.LIST_COLOR);
			mTaskNameColumn = c.getColumnIndex(TaskContract.TaskListColumns.LIST_NAME);
			mAccountNameColumn = c.getColumnIndex(TaskContract.TaskListSyncColumns.ACCOUNT_NAME);
		}
	}


	@Override
	public void bindView(View v, Context context, Cursor c)
	{
		/* Since we override getView and get DropDownView we don't need this method. */
	}


	@Override
	public View newView(Context context, Cursor c, ViewGroup vg)
	{
		/* Since we override getView and get DropDownView we don't need this method. */
		return null;
	}


	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		if (convertView == null)
		{
			convertView = mInflater.inflate(R.layout.list_spinner_item_selected, null);
		}

		TextView listName = (TextView) convertView.findViewById(R.id.task_list_name);
		listName.setTextColor(Color.parseColor("#000000"));
//		.setBackground(new ColorDrawable(Color.parseColor("#59865F")));
		TextView accountName = (TextView) convertView.findViewById(R.id.task_list_account_name);
		accountName.setTextColor(Color.parseColor("#000000"));
		Cursor cursor = (Cursor) getItem(position);

		listName.setText(cursor.getString(mTaskNameColumn));
		accountName.setText(cursor.getString(mAccountNameColumn));
		int taskListColor = cursor.getInt(mTaskColorColumn);
		int backgroundBasedColor = AbstractFieldView.getTextColorFromBackground(taskListColor);

        convertView.setBackground(new ColorDrawable(Color.parseColor("#59865F")));
//		listName.setTextColor(backgroundBasedColor);
//		accountName.setTextColor(backgroundBasedColor);
		return convertView;
	}


	public View getDropDownView(int position, View convertView, ViewGroup parent)
	{
		if (convertView == null)
		{
			convertView = mInflater.inflate(R.layout.list_spinner_item_dropdown, null);

		}

		View listColor = convertView.findViewById(R.id.task_list_color);
		TextView listName = (TextView) convertView.findViewById(R.id.task_list_name);
		TextView accountName = (TextView) convertView.findViewById(R.id.task_list_account_name);
		Cursor cursor = (Cursor) getItem(position);

		listColor.setBackgroundColor(cursor.getInt(mTaskColorColumn));
		listName.setText(cursor.getString(mTaskNameColumn));
		accountName.setText(cursor.getString(mAccountNameColumn));
		return convertView;
	}
}
