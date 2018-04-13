package eu.operando.adapter;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.List;

import eu.operando.R;

/**
 * Created by Alex on 12.04.2018.
 */

public class SocialNetworkAccountsAdapter extends ArrayAdapter<String> {

    private Context context;
    private String[] objects = new String[]{
            "Facebook", "Linkedin", "Twitter", "Google"
    };
    private int[] logos = new int[]{
            R.drawable.facebook_menu_item, R.drawable.linkedin_menu_item, R.drawable.twitter_menu_item, R.drawable.google_menu_item
    };
    private String[] domains = new String[]{
            "https://m.facebook.com",
            "https://www.linkedin.com",
            "https://twitter.com",
            "https://myaccount.google.com",
            "https://mobile.twitter.com",
            "https://api.twitter.com",
    };

    public SocialNetworkAccountsAdapter(@NonNull Context context, int resource) {
        super(context, resource);
        this.context = context;
    }

    @Override
    public int getCount() {
        return objects.length;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        final CustomViewHolder holder;

        if (convertView == null) {

            LayoutInflater inflater = (LayoutInflater)
                    context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.social_networks_account_item, null);

            holder = new CustomViewHolder(convertView);

            convertView.setTag(holder);
        } else {
            holder = (CustomViewHolder) convertView.getTag();
        }

        holder.setData(position);

        return convertView;

    }

    private class CustomViewHolder extends RecyclerView.ViewHolder {

        TextView tv;
        Button logoutBtn;
        ImageView logo;
        LinearLayout accountItem;

        CustomViewHolder(View itemView) {
            super(itemView);

            tv = (TextView) itemView.findViewById(R.id.sn_tv);
            logoutBtn = (Button) itemView.findViewById(R.id.sn_btn);
            logo = (ImageView) itemView.findViewById(R.id.sn_logo);
            accountItem = (LinearLayout) itemView.findViewById(R.id.item_view);
        }

        public void setData(final int position) {

            tv.setText(objects[position]);
            logo.setBackgroundResource(logos[position]);

            if (!isLogged(position)) {
                logoutBtn.setVisibility(View.GONE);
                accountItem.setBackgroundColor(ContextCompat.getColor(context, R.color.social_network_settings_header_background_opaque));
            } else {
                accountItem.setBackgroundColor(ContextCompat.getColor(context, R.color.social_network_settings_header_background));
                logoutBtn.setVisibility(View.VISIBLE);
                logoutBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        deleteCookieForDomain(position);
                    }
                });
            }

        }

        private boolean isLogged(int position) {

            CookieManager cookieManager = CookieManager.getInstance();
            String cookiestring = cookieManager.getCookie(domains[position]);
            return cookiestring != null;
        }
    }

    public void clearCookies(String domain) {

        CookieSyncManager csm = CookieSyncManager.createInstance(context);
        CookieManager cookieManager = CookieManager.getInstance();
        String cookiestring = cookieManager.getCookie(domain);
        String[] cookies = cookiestring.split(";");
        for (int i = 0; i < cookies.length; i++) {
            String[] cookieparts = cookies[i].split("=");
            cookieManager.setCookie(domain, cookieparts[0].trim() + "=;");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.flush();
        }
        CookieSyncManager.getInstance().sync();

        Log.e("bla", "bla");
    }

    private void deleteWebViewCookiesForDomain(String domain) {
        CookieSyncManager csm = CookieSyncManager.createInstance(context);
        CookieManager cm = CookieManager.getInstance();

    /* http://code.google.com/p/android/issues/detail?id=19294 */
        if (Build.VERSION.SDK_INT < 11) {
            if (domain.startsWith(".")) domain = domain.substring(1);
        }

    /* Cookies are stored by domain, and are not different for different schemes (i.e. http vs
     * https) (although they do have an optional 'secure' flag.) */
        String cookieGlob = cm.getCookie(domain);
        if (cookieGlob != null) {
            String[] cookies = cookieGlob.split(";");
            for (String cookieTuple : cookies) {
                String[] cookieParts = cookieTuple.split("=");

            /* setCookie has changed a lot between different versions of Android with respect to
             * how it handles cookies like these, which are set in order to clear an existing
             * cookie.  This way of invoking it seems to work on all versions. */
                cm.setCookie(domain, cookieParts[0] + "=;");
            /* These calls have worked for some subset of the the set of all versions of
             * Android:
             */
//            cm.setCookie(domain, cookieParts[0] + "=");
//              cm.setCookie(domain, cookieParts[0]);
            }
            csm.sync();
            if (Build.VERSION.SDK_INT > 20) {
                cm.flush();
            }
        }
    }

    private void deleteCookieForDomain(int position) {
        CookieSyncManager csm = CookieSyncManager.createInstance(context);
        CookieManager cm = CookieManager.getInstance();

        List<Pair<String, String>> otherDomains = new ArrayList<>();
        int length = domains.length;
        if (position == 2) {
            length -= 2;
        }
        for (int i = 0; i < length; ++i) {
            if (position != i)
                otherDomains.add(new Pair<>(domains[i], cm.getCookie(domains[i])));
        }

        for (int i = 0; i < otherDomains.size(); ++i) {
            Log.e("1[Domain]" + otherDomains.get(i).first, "[Cookie]" + otherDomains.get(i).second);
        }

        cm.removeAllCookie();

        for (int i = 0; i < otherDomains.size(); ++i) {
            Log.e("2[Domain]" + otherDomains.get(i).first, "[Cookie]" + otherDomains.get(i).second);

            if (otherDomains.get(i).second != null) {
                String[] cookies = otherDomains.get(i).second.split(";");
                for (String cookieTuple : cookies) {
                    cm.setCookie(otherDomains.get(i).first, cookieTuple);
                }
            }
        }

        csm.sync();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            cm.flush();
//        }

        for (int i = 0; i < otherDomains.size(); ++i) {
            Log.e("3[Domain]" + otherDomains.get(i).first, "[Cookie]" + cm.getCookie(otherDomains.get(i).first));
        }

    }
}
