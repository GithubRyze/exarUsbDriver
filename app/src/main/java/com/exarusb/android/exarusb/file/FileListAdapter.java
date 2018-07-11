/*
 *  
 * Copyright 2013 Exar Corporation
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
 */

package com.exarusb.android.exarusb.file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.exarusb.android.exarusb.R;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * 
 * @author Exar Corporation
 *
 * Send File Activity implementation
 */
public class FileListAdapter extends BaseAdapter {
	private Context mContext;
	private List<File> mObjects = new ArrayList<File>();

	public FileListAdapter(Context context) {
		mContext = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
		if (null == convertView) {
			convertView = View.inflate(mContext, R.layout.fc_item_file_list, null);
			viewHolder = new ViewHolder();
			viewHolder.icon = (ImageView) convertView.findViewById(R.id.fc_item_icon);
			viewHolder.name = (TextView) convertView.findViewById(R.id.fc_item_name);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}

		File file = mObjects.get(position);
		if (file.isFile()) {
			viewHolder.icon.setImageResource(R.drawable.ic_file);
			

		} else {
			viewHolder.icon.setImageResource(R.drawable.ic_folder);
		}

		viewHolder.name.setText(file.getName());
		return convertView;
	}

	public void setObjects(List<File> objects) {
		mObjects = objects;
		notifyDataSetChanged();
	}

	public void clear() {
		mObjects.clear();
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return mObjects.size();
	}

	@Override
	public Object getItem(int position) {
		return mObjects.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	private static class ViewHolder {
		public ImageView icon;
		public TextView name;

	}
}

