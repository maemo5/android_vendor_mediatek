package com.mediatek.rcs.pam.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.vcard.VCardEntry;
import com.android.vcard.VCardEntryHandler;
import com.mediatek.rcs.message.location.GeoLocXmlParser;
import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.MediaFolder;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.Utils;
import com.mediatek.rcs.pam.model.MediaArticle;
import com.mediatek.rcs.pam.model.MessageContent;
import com.mediatek.rcs.pam.util.PaVcardParserResult;
import com.mediatek.rcs.pam.util.PaVcardUtils;

import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AccountHistoryMsgUtils {

    private static String TAG = Constants.TAG_PREFIX + "AccountHistoryMsgUtils";
    private static final int SIZE_VCARD_CACHE = 50;

    private Context mContext;
    private ImageCustomizedLoader mImageLoader;
    private IAudioDownloader mAudioDownloader;
    private LruCache<String, String> mVcardPathCache;

    public AccountHistoryMsgUtils(Context context, ClientQueryActivity.Downloader downloader) {
        mContext = context;
        mImageLoader = new ImageCustomizedLoader(mContext, downloader);
        mAudioDownloader = new AccountAudioDownloader(downloader);
        mVcardPathCache = new LruCache<String, String>(SIZE_VCARD_CACHE);
    }

    public IAudioDownloader getAudioDownloader() {
        return mAudioDownloader;
    }

    public void updateText(TextView content, MessageContent message) {
        content.setText(message.text);
        content.setVisibility(View.VISIBLE);
    }

    public void updateImageOrVideoView(LinearLayout layout, final MessageContent message) {
        layout.setVisibility(View.VISIBLE);

        ImageView mImageContent = (ImageView) layout.findViewById(R.id.image_content);
        TextView mContentSize = (TextView) layout.findViewById(R.id.content_size);
        ImageView playButton = (ImageView) layout.findViewById(R.id.video_media_paly);

        mImageLoader.updateImage(mImageContent, message.basicMedia.thumbnailUrl);
        mContentSize.setText(message.basicMedia.fileSize);

        if (message.mediaType == Constants.MEDIA_TYPE_VIDEO) {
            playButton.setVisibility(View.VISIBLE);
        } else {
            playButton.setVisibility(View.INVISIBLE);
        }

        layout.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showImageOrVideo(message);
            }
        });
    }

    public void updateGeoView(ImageView imageView, final MessageContent message) {
        imageView.setVisibility(View.VISIBLE);

        imageView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                openGeoLocation(message);
            }
        });
    }

    public void updateVcard(LinearLayout layout, final MessageContent message) {
        layout.setVisibility(View.VISIBLE);

        final String filePath = saveVcardToFile(message);
        if (filePath != null) {
            parseVcardViews(layout, filePath);
        }

        layout.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                openVcard(filePath);
            }
        });
    }

    private String saveVcardToFile(MessageContent message) {
        int type = Constants.MEDIA_TYPE_VCARD;
        String extension = ".vcf";

        String filePath = mVcardPathCache.get(message.text);
        if (filePath != null) {
            File file = new File(filePath);
            if (file.exists()) {
                Log.i(TAG, "[vcard] in cache " + filePath);
                return mVcardPathCache.get(message.text);
            }
        }

        filePath = MediaFolder.generateMediaFileName(Constants.INVALID, type, extension);
        try {
            Utils.storeToFile(message.text, filePath);
            mVcardPathCache.put(message.text, filePath);
            Log.i(TAG, "[vcard] new file, add to cache " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return filePath;
    }

    private void parseVcardViews(LinearLayout layout, String filePath) {
        TextView info = (TextView) layout.findViewById(R.id.ip_vcard_info);
        ImageView icon = (ImageView) layout.findViewById(R.id.ip_vcard_icon);

        CustomVCardEntryHandler entryHandler = new CustomVCardEntryHandler(mContext, icon, info);

            int vcardCount = PaVcardUtils.getVcardEntryCount(filePath);
            Log.i(TAG, "Contact number: " + vcardCount);

            if (vcardCount == 1) {
                info.setText(R.string.contact_name);
                PaVcardUtils.parseVcard(filePath, entryHandler);
            } else {
                info.setText(R.string.multi_contacts_name);
            }
    }

    public void updateSingleArticleView(LinearLayout layout, MessageContent message) {
        layout.setVisibility(View.VISIBLE);
        MediaArticle article = message.article.get(0);

        TextView titleView = (TextView) layout.findViewById(R.id.text_title);
        TextView introView = (TextView) layout.findViewById(R.id.text_intro);
        ImageView imageView = (ImageView) layout.findViewById(R.id.img_logo);

        titleView.setText(article.title);
        introView.setText(article.mainText);
        mImageLoader.updateImage(imageView, article.originalUrl);

        addListenOpenLink(layout, article.sourceUrl);
    }

    public ArrayList<View> updateMultiArticleView(LinearLayout layout, MessageContent message) {
        layout.setVisibility(View.VISIBLE);

        ArrayList<View> multiItemList = new ArrayList<View>();

        List<MediaArticle> articleList = message.article;
        updateMultiHeader(layout, articleList.get(0));

        for (int i = 1; i < articleList.size(); ++i) {
            LinearLayout item = buildMultiItem(articleList.get(i));
            multiItemList.add(item);
            layout.addView(item, i);
        }
        return multiItemList;
    }

    private RelativeLayout updateMultiHeader(View parentView, MediaArticle article) {
        Log.i(TAG, "updateMultiHeader");

        RelativeLayout header = (RelativeLayout) parentView.findViewById(R.id.rl_multi_header);
        TextView text = (TextView) header.findViewById(R.id.tv_header_title);
        ImageView imageView = (ImageView) header.findViewById(R.id.iv_header_thumb);

        text.setText(article.title);
        mImageLoader.updateImage(imageView, article.originalUrl);

        addListenOpenLink(header, article.sourceUrl);
        return header;
    }

    private LinearLayout buildMultiItem(MediaArticle article) {
        Log.i(TAG, "buildMultiItem");

        LinearLayout item = (LinearLayout) parseLayout(R.layout.account_history_item_multi_body);
        TextView text = (TextView) item.findViewById(R.id.tv_mixed_multi_title);
        ImageView imageView = (ImageView) item.findViewById(R.id.iv_mixed_multi_thumb);

        text.setText(article.title);
        mImageLoader.updateImage(imageView, article.originalUrl);

        addListenOpenLink(item, article.sourceUrl);
        return item;
    }

    /*
     * Show image or video via PAIpMsgContentShowActivity
     */
    private void showImageOrVideo(final MessageContent message) {
        int type = message.mediaType;
        if (type == Constants.MEDIA_TYPE_PICTURE || type == Constants.MEDIA_TYPE_VIDEO) {
            Intent intent = new Intent(mContext, PAIpMsgContentShowActivity.class);
            intent.putExtra("type", type);
            intent.putExtra("thumbnail_url", message.basicMedia.thumbnailUrl);
            intent.putExtra("thumbnail_path", message.basicMedia.thumbnailPath);
            intent.putExtra("original_url", message.basicMedia.originalUrl);
            intent.putExtra("original_path", message.basicMedia.originalPath);
            mContext.startActivity(intent);
    }
    }

    /*
     * Open vcard info via Contacts
     */
    private void openVcard(final String filePath) {
        if (filePath == null) {
            Toast.makeText(mContext, "file path is null", Toast.LENGTH_SHORT).show();
            return;
        }

        int entryCount = PaVcardUtils.getVcardEntryCount(filePath);
        Log.d(TAG, "vCard entryCount=" + entryCount);
        if (entryCount <= 0) {
            Toast.makeText(mContext, "parse vCard error", Toast.LENGTH_LONG).show();

        } else if (entryCount == 1) {
            Uri uri = Uri.parse("file://" + filePath);
            Intent vcardIntent = new Intent("android.intent.action.rcs.contacts.VCardViewActivity");
        vcardIntent.setDataAndType(uri, "text/x-vcard");
            vcardIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            mContext.startActivity(vcardIntent);

        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle(R.string.multi_contacts_name)
                    .setMessage(mContext.getString(R.string.multi_contacts_notification))
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Uri uri = Uri.parse("file://" + filePath);
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(uri, "text/x-vcard");
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            mContext.startActivity(intent);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).create().show();
        }
    }

    /*
     * Parse location latitude/longitude info and show another map app
     */
    private void openGeoLocation(MessageContent message) {
        Log.d(TAG, "message text is " + message.text);

        InputSource geoloc = new InputSource(new ByteArrayInputStream(message.text.getBytes()));
        GeoLocXmlParser parser;
        try {
            parser = new GeoLocXmlParser(geoloc);
            Log.d(TAG, "parser is " + parser);

            double latitude = parser.getLatitude();
            double longitude = parser.getLongitude();
            Log.d(TAG, "parseGeoLocXml: latitude=" + latitude + ", longtitude=" + longitude);

            if (latitude != 0.0 || longitude != 0.0) {
                Uri uri = Uri.parse("geo:" + latitude + "," + longitude);
                Intent locIntent = new Intent(Intent.ACTION_VIEW, uri);
                mContext.startActivity(locIntent);
            } else {
                Toast.makeText(mContext, "parse geoloc info fail", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Add a click listener to open web link via PaWebViewActivity
     */
    private void addListenOpenLink(View view, final String link) {
        Log.i(TAG, "addClickLinkListener");

        view.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                PaWebViewActivity.openHyperLink(mContext, link);
            }
        });
    }

    /*
     * Inflate xml layout to a view.
     */
    private View parseLayout(int resId) {
        return LayoutInflater.from(mContext).inflate(resId, null);
    }

    /*
     * Convert the long value to human readable format.
     */
    public static String getFormatterTime(long timestamp) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm",
                Locale.getDefault());
        String formatted = simpleDateFormat.format(timestamp);
        return formatted;
    }

    static class CustomVCardEntryHandler implements VCardEntryHandler {

        private ImageView mIcon;
        private TextView mTitle;
        private Context mContext;

        public CustomVCardEntryHandler(Context context, ImageView icon, TextView title) {
            mContext = context;
            mIcon = icon;
            mTitle = title;
        }

        @Override
        public void onEnd() {
        }

        @Override
        public void onEntryCreated(VCardEntry entry) {
            PaVcardParserResult result = PaVcardUtils.ParserRcsVcardEntry(entry, mContext);

            mTitle.setText(result.getName());
            byte[] pic = result.getPhoto();
            if (pic != null) {
                Bitmap vcardBitmap = BitmapFactory.decodeByteArray(pic, 0, pic.length);
                mIcon.setImageBitmap(vcardBitmap);
            }
        }

        @Override
        public void onStart() {
        }
    }
}
