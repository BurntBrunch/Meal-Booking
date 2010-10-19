/**
 * 
 */
package name.kratunov.mealbooking;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;

/**
 * @author archivator
 *
 */
public class DecapsulatedSimpleAdapter extends SimpleAdapter {
	private OnClickListener mListener;
	private OnLongClickListener mLongListener;
	public DecapsulatedSimpleAdapter(Context context,
			List<? extends Map<String, ?>> data, int resource, String[] from,
			int[] to) {
		super(context, data, resource, from, to);
	}
	public void setOnClickListener(OnClickListener listener)
	{
		mListener = listener;
	}
	public void setOnLongClickListener(OnLongClickListener listener)
	{
		mLongListener = listener;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View ret = super.getView(position, convertView, parent);
		OnClickListener listener = mListener;
		OnLongClickListener longlistener = mLongListener;
		if(mListener!=null)
		{
			if (ret instanceof ViewGroup)
			{
				ViewGroup retg = (ViewGroup) ret;
				for(int i=0; i<retg.getChildCount(); i++)
				{
					retg.getChildAt(i).setOnClickListener(listener);
					if(longlistener!=null)
						retg.getChildAt(i).setOnLongClickListener(longlistener);
				}
			}
			ret.setOnClickListener(mListener);
		}
		return ret;
	}

}
