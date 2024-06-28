package com.fitprinting;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.ArrayList;
import java.util.List;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.content.Context;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import java.text.DecimalFormat;
import android.os.Looper;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.Callback;
import com.fujitsu.fitPrint.Library.FitPrintAndroidLan_v1102.FitPrintAndroidLan;

import com.facebook.react.bridge.Promise;
import android.util.Log;
import java.util.Arrays;

public class FitPrintingModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;

    public FitPrintingModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    FitPrintAndroidLan mPrinter = new FitPrintAndroidLan();
    Canvas mainCanvas;
    Bitmap mainBitmap;
    Paint fillPaint = new Paint();
    TextPaint textPaint = new TextPaint();
    int defaultWidth = 600;
    float printHeight;
    ListBitmapPrint listBitmap;

    @ReactMethod
    public void setDefaultWidth(int width) {
        this.defaultWidth = width;
    }

    @Override
    public String getName() {
        return "FitPrinting";
    }

    @ReactMethod
    public void sampleMethod(String stringArgument, int numberArgument, Callback callback) {
        // TODO: Implement some real useful functionality
        callback.invoke("Received numberArgument: " + numberArgument + " stringArgument: " + stringArgument);
    }

    @ReactMethod
    public void printBill(String ip, ReadableMap data, Promise promise) {
        try {
            ReadableArray a = data.getArray("listOrigin");
            ReadableArray c = data.getArray("listTotal");
            ReadableMap d = data.getMap("labelInfo");
            Bitmap b;
            Connect(ip);
            b = createLogo(d.getString("orderId").split("_")[0]);
            mPrinter.PrintImage(b);
            b = createInfo("Mã đơn hàng:", d.getString("orderId"), true);
            mPrinter.PrintImage(b);
            b = createInfo("Phuơng thức thanh toán: ", d.getString("paymentMethod"), false);
            mPrinter.PrintImage(b);
            b = createInfo("Phuơng thức vận chuyển:", d.getString("shippingMethod"), false);
            mPrinter.PrintImage(b);
            b = createInfo("Ngày đặt hàng:", d.getString("created_at"), false);
            mPrinter.PrintImage(b);
            b = createInfo("Ngày in phiếu giao hàng:", d.getString("date"), false);
            mPrinter.PrintImage(b);
            b = createDeliveryInfo("Thông tin thanh toán:", true);
            mPrinter.PrintImage(b);
            b = createDeliveryInfo(d.getString("billingName"), false);
            mPrinter.PrintImage(b);
            b = createDeliveryInfo(d.getString("billingAddress"), false);
            mPrinter.PrintImage(b);
            b = createDeliveryInfo(d.getString("billingPhone"), false);
            mPrinter.PrintImage(b);
            b = createDeliveryInfo("Thông tin nhận hàng:", true);
            mPrinter.PrintImage(b);
            b = createDeliveryInfo(d.getString("shippingName"), false);
            mPrinter.PrintImage(b);
            b = createDeliveryInfo(d.getString("shippingAddress"), false);
            mPrinter.PrintImage(b);
            b = createDeliveryInfo(d.getString("shippingPhone"), false);
            mPrinter.PrintImage(b);

            mPrinter.PrintLogo(2, 1, 1);

            for (int i = 0; i < a.size(); i++) {
                ReadableMap temp = a.getMap(i);
                b = newBookImage(String.valueOf(i + 1), temp.getString("productName"), temp.getString("quantity"),
                        temp.getString("originalPrice"), temp.getString("discount") + "%", temp.getString("price"));
                mPrinter.PrintImage(b);
            }
            mPrinter.PrintText("_______________________________________________", "SJIS");
            mPrinter.PrintText("_______________________________________________", "SJIS");
            for (int i = 0; i < c.size(); i++) {
                ReadableMap temp = c.getMap(i);
                if (temp.hasKey("originalCost")) {
                    b = total("Tổng cộng:", temp.getString("originalCost"), false);
                    mPrinter.PrintImage(b);
                }
                if (temp.hasKey("shipFee")) {
                    b = total("Phí vận chuyển:", temp.getString("shipFee"), false);
                    mPrinter.PrintImage(b);
                }
                if (temp.hasKey("wrapFee")) {
                    b = total("Phí gói quà:", "-" + temp.getString("wrapFee"), false);
                    mPrinter.PrintImage(b);
                }
                if (temp.hasKey("discountAmount")) {
                    b = total("Giảm giá thêm:", "-" + temp.getString("discountAmount"), false);
                    mPrinter.PrintImage(b);
                }
                if (temp.hasKey("fpoint")) {
                    b = total("Fpoint:", "-" + temp.getString("fpoint"), false);
                    mPrinter.PrintImage(b);
                }
                if (temp.hasKey("fhsCoin")) {
                    b = total("Code điện tử:", "-" + temp.getString("fhsCoin"), false);
                    mPrinter.PrintImage(b);
                }
                if (temp.hasKey("finalCost")) {
                    b = total("Tổng thanh toán:", temp.getString("finalCost"), true);
                    mPrinter.PrintImage(b);
                }
            }

            // Print eInvoice instruction
            if (d.getString("eInvoiceRef") != null) {
                mPrinter.PrintText("    ", "SJIS");
                mPrinter.PrintText("================================================", "SJIS");
                b = generateInvoiceFooter(d.getString("eInvoiceUrl"), d.getString("eInvoiceRef"));
                mPrinter.PrintImage(b);
            }
            mPrinter.PaperFeed(200);
            mPrinter.CutPaper(0);
            mPrinter.Disconnect();
            promise.resolve("success");
        } catch (Exception e) {
            mPrinter.Disconnect();
            promise.reject(e.getMessage());
        }
    }

    @ReactMethod
    public void printGrab(String ip, ReadableMap data, Promise promise) {
        try {
            String name = data.getString("name");
            String phone = data.getString("phone");
            String licensePlate = data.getString("licensePlate");
            Bitmap b;
            Connect(ip);

            b = createGrabLabel(name, phone, licensePlate);
            mPrinter.PrintImage(b);
            mPrinter.PaperFeed(64);

            mPrinter.CutPaper(0);
            mPrinter.Disconnect();
            promise.resolve("success");
        } catch (Exception e) {
            mPrinter.Disconnect();
            promise.reject(e.getMessage());
        }
    }

    public Bitmap generateInvoiceFooter(String url, String code) {
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        TextPaint tp = new TextPaint();
        tp.setColor(Color.BLACK);
        tp.setTextAlign(Paint.Align.LEFT);
        tp.setAntiAlias(true);
        tp.setTypeface(Typeface.create("Times New Roman", Typeface.BOLD));
        tp.setTextSize(25);

        StaticLayout InvoiceInfo = new StaticLayout("Quý khách tra cứu hóa đơn điện tử bằng mã ", tp, 600,
                Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
        Bitmap image = Bitmap.createBitmap(600, 300, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawPaint(paint);
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.create("Times New Roman", Typeface.NORMAL));
        paint.setTextSize(20);
        canvas.drawText("Quý khách tra cứu hóa đơn điện tử bằng mã", 0, 20, paint);
        canvas.drawText("tại đường dẫn: ", 0, 45, paint);
        canvas.drawText(" hoặc quét mã QR sau : ", 0, 70, paint);
        paint.setTypeface(Typeface.create("Times New Roman", Typeface.BOLD));
        canvas.drawText(code, 410, 20, paint);
        canvas.drawText(url, 190, 45, paint);

        try {
            Bitmap qrcode = TextToImageEncode(url + "?sec=" + code);
            canvas.drawBitmap(qrcode, 190, 95, paint);
        } catch (WriterException e) {
            e.printStackTrace();
        }

        // InvoiceInfo.draw(canvas);
        return image;
    }

    public Bitmap createGrabLabel(String name, String phone, String licensePlate) throws Exception {
        Paint paint2 = new Paint();
        paint2.setColor(Color.WHITE);
        paint2.setStyle(Paint.Style.FILL);
        TextPaint tp = new TextPaint();
        tp.setColor(Color.BLACK);
        tp.setTextAlign(Paint.Align.LEFT);
        tp.setAntiAlias(true);
        tp.setTypeface(Typeface.create("Times New Roman", Typeface.BOLD));

        tp.setTextSize(100);
        StaticLayout Name = new StaticLayout(name, tp, 1000, Layout.Alignment.ALIGN_CENTER, 1, 0, false);

        StaticLayout Phone = new StaticLayout(phone, tp, 1000, Layout.Alignment.ALIGN_CENTER, 1, 0, false);

        // tp.setTextSize(50);
        StaticLayout LicensePlate = new StaticLayout(licensePlate, tp, 1000, Layout.Alignment.ALIGN_CENTER, 1, 0,
                false);

        Bitmap image = Bitmap.createBitmap(1000, 500, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawPaint(paint2);
        Name.draw(canvas);
        canvas.translate(0, 150);
        Phone.draw(canvas);
        canvas.translate(0, 150);
        LicensePlate.draw(canvas);

        return RotateBitmap(image, 90);
    }

    public static Bitmap RotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public void Connect(String ip) throws Exception {
        final String tempIp = ip;
        final Runnable stuffToDo = new Thread() {
            @Override
            public void run() {
                mPrinter.Connect(tempIp);
            }
        };

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future future = executor.submit(stuffToDo);
        executor.shutdown(); // This does not cancel the already-scheduled task.

        future.get(1, TimeUnit.SECONDS);

        if (!executor.isTerminated())
            executor.shutdownNow(); // If you want to stop the code that hasn't finished.
    }

    @ReactMethod
    public void POSInit(String ip, Promise promise) {
        try {
            Connect(ip);
            mPrinter.Beep(255, 255);
            Bitmap image;
            image = createLogo2();
            mPrinter.SetLogo(image, 2);
            mPrinter.Disconnect();
            promise.resolve("success");
        } catch (Exception e) {
            mPrinter.Disconnect();
            promise.reject(e);
        }

    }

    public static String priceWithoutDecimal(Double price) {
        DecimalFormat formatter = new DecimalFormat("###,###,###.##");
        return formatter.format(price);
    }

    public Bitmap newBookImage(String no, String tittle, String qty, String cost, String ck, String Total)
            throws Exception {
        Total = Total.split("\\.")[0];
        cost = cost.split("\\.")[0];
        Total = String.valueOf(Integer.parseInt(Total) * Integer.parseInt(qty));
        Total = priceWithoutDecimal(Double.parseDouble(Total));
        cost = priceWithoutDecimal(Double.parseDouble(cost));
        Total = replace(Total);
        cost = replace(cost);

        Paint paint2 = new Paint();
        paint2.setColor(Color.WHITE);
        paint2.setStyle(Paint.Style.FILL);
        TextPaint tp = new TextPaint();
        tp.setColor(Color.BLACK);
        tp.setTextSize(22);
        tp.setTextAlign(Paint.Align.LEFT);
        tp.setAntiAlias(true);

        StaticLayout No = new StaticLayout(no + ".   ", tp, 40, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
        StaticLayout title = new StaticLayout(tittle, tp, 530, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
        StaticLayout SL = new StaticLayout(qty, tp, 50, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
        StaticLayout costL = new StaticLayout(cost, tp, 100, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
        StaticLayout CK = new StaticLayout(ck, tp, 100, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
        StaticLayout totalL = new StaticLayout(Total, tp, 100, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
        Bitmap image = Bitmap.createBitmap(600, (title.getLineCount() + 1) * 35, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawPaint(paint2);
        No.draw(canvas);
        canvas.translate(40, 0);
        title.draw(canvas);
        canvas.translate(180, 15 + 25 * title.getLineCount());
        SL.draw(canvas);
        canvas.translate(40, 0);
        costL.draw(canvas);
        canvas.translate(120, 0);
        CK.draw(canvas);
        canvas.translate(90, 0);
        totalL.draw(canvas);
        return image;
    }

    public String replace(String temp) throws Exception {
        temp.replace(",", ".");
        return temp;
    }

    public Bitmap total(String key, String value, boolean bold) throws Exception {
        value = value.split("\\.")[0];
        value = priceWithoutDecimal(Double.parseDouble(value));
        value = replace(value);
        Paint paint2 = new Paint();
        paint2.setColor(Color.WHITE);
        paint2.setStyle(Paint.Style.FILL);
        TextPaint tp = new TextPaint();
        tp.setColor(Color.BLACK);
        tp.setTextSize(20);
        tp.setTextAlign(Paint.Align.LEFT);
        tp.setAntiAlias(true);
        if (bold) {
            tp.setTypeface(Typeface.create("Times New Roman", Typeface.BOLD));

        }

        StaticLayout Key = new StaticLayout(key, tp, 200, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
        StaticLayout Value = new StaticLayout(value, tp, 200, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
        Bitmap image = Bitmap.createBitmap(600, 30, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawPaint(paint2);
        canvas.translate(250, 0);
        Key.draw(canvas);
        canvas.translate(210, 0);
        Value.draw(canvas);
        return image;
    }

    public Bitmap createLogo(String Value) throws Exception {

        Paint paint2 = new Paint();
        paint2.setColor(Color.WHITE);
        paint2.setStyle(Paint.Style.FILL);
        TextPaint tp = new TextPaint();
        tp.setColor(Color.BLACK);
        tp.setTextSize(45);
        tp.setTypeface(Typeface.create("Times New Roman", Typeface.BOLD));
        tp.setTextAlign(Paint.Align.LEFT);
        tp.setAntiAlias(true);
        TextPaint tp2 = new TextPaint();
        tp2.setColor(Color.BLACK);
        tp2.setTextSize(18);
        tp2.setTextAlign(Paint.Align.LEFT);
        tp2.setAntiAlias(true);
        TextPaint tp3 = new TextPaint();
        tp3.setTypeface(Typeface.create("Times New Roman", Typeface.BOLD));
        tp3.setColor(Color.BLACK);
        tp3.setTextSize(40);
        tp3.setTextAlign(Paint.Align.LEFT);
        tp3.setAntiAlias(true);
        TextPaint tp4 = new TextPaint();
        tp4.setColor(Color.BLACK);
        tp4.setTextSize(20);
        tp4.setTextAlign(Paint.Align.LEFT);
        tp4.setAntiAlias(true);
        StaticLayout logo = new StaticLayout("FAHASA.COM", tp, 400, Layout.Alignment.ALIGN_CENTER, 1, 0, false);
        StaticLayout address = new StaticLayout("Hotline: 1900 636467\nEmail: sales@fahasa.com", tp2, 250,
                Layout.Alignment.ALIGN_CENTER, 1, 0, false);
        Bitmap image = Bitmap.createBitmap(600, 200, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawPaint(paint2);
        canvas.translate(-40, 10);
        logo.draw(canvas);
        canvas.translate(60, 60);
        address.draw(canvas);
        canvas.translate(330, -105);

        Bitmap qrcode = null;
        try {
            qrcode = TextToImageEncode(Value);

        } catch (WriterException e) {
            e.printStackTrace();
        }
        canvas.drawBitmap(qrcode, 0, 0, tp2);
        return image;
    }

    private Bitmap TextToImageEncode(String Value) throws WriterException {
        BitMatrix bitMatrix;
        try {
            bitMatrix = new MultiFormatWriter().encode(Value, BarcodeFormat.DATA_MATRIX.QR_CODE, 200, 200, null);

        } catch (IllegalArgumentException Illegalargumentexception) {

            return null;
        }
        int bitMatrixWidth = bitMatrix.getWidth();

        int bitMatrixHeight = bitMatrix.getHeight();

        int[] pixels = new int[bitMatrixWidth * bitMatrixHeight];

        for (int y = 0; y < bitMatrixHeight; y++) {
            int offset = y * bitMatrixWidth;

            for (int x = 0; x < bitMatrixWidth; x++) {

                pixels[offset + x] = bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_4444);

        bitmap.setPixels(pixels, 0, 200, 0, 0, bitMatrixWidth, bitMatrixHeight);
        return bitmap;
    }

    public Bitmap createLogo2() {
        Paint paint2 = new Paint();
        paint2.setColor(Color.WHITE);
        paint2.setStyle(Paint.Style.FILL);
        TextPaint tp = new TextPaint();
        tp.setColor(Color.BLACK);
        tp.setTextSize(50);
        tp.setTextAlign(Paint.Align.LEFT);
        tp.setAntiAlias(true);
        TextPaint tp2 = new TextPaint();
        tp2.setColor(Color.BLACK);
        tp2.setTextSize(18);
        tp2.setTextAlign(Paint.Align.LEFT);
        tp2.setAntiAlias(true);
        TextPaint tp3 = new TextPaint();
        tp3.setTypeface(Typeface.create("Times New Roman", Typeface.BOLD));
        tp3.setColor(Color.BLACK);
        tp3.setTextSize(35);
        tp3.setTextAlign(Paint.Align.LEFT);
        tp3.setAntiAlias(true);
        TextPaint tp4 = new TextPaint();
        tp4.setColor(Color.BLACK);
        tp4.setTextSize(20);
        tp4.setTextAlign(Paint.Align.LEFT);
        tp4.setAntiAlias(true);

        StaticLayout bill = new StaticLayout("Chi tiết đơn hàng  ________________________________", tp3, 600,
                Layout.Alignment.ALIGN_CENTER, 1, 0, false);
        StaticLayout col = new StaticLayout(
                "STT   Tên                          SL        Giá               CK          Thành tiền   ______________________________________________________________",
                tp4, 600, Layout.Alignment.ALIGN_CENTER, 1, 0, false);
        Bitmap image = Bitmap.createBitmap(600, 180, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawPaint(paint2);
        bill.draw(canvas);
        canvas.translate(-20, 100);
        col.draw(canvas);
        return image;
    }

    public Bitmap createInfo(String key, String value, boolean bold) throws Exception {
        Paint paint2 = new Paint();
        paint2.setColor(Color.WHITE);
        paint2.setStyle(Paint.Style.FILL);
        TextPaint tp = new TextPaint();
        tp.setColor(Color.BLACK);
        tp.setTextSize(22);

        tp.setTextAlign(Paint.Align.LEFT);
        tp.setAntiAlias(true);

        StaticLayout Key = new StaticLayout(key, tp, 250, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
        if (bold) {
            tp.setTypeface(Typeface.create("Times New Roman", Typeface.BOLD));

        }
        StaticLayout Value = new StaticLayout(value, tp, 330, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
        Bitmap image = Bitmap.createBitmap(600, Value.getLineCount() * 30, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawPaint(paint2);
        Key.draw(canvas);
        canvas.translate(250, 0);
        Value.draw(canvas);
        return image;
    }

    public Bitmap createDeliveryInfo(String key, boolean bold) throws Exception {
        Paint paint2 = new Paint();
        paint2.setColor(Color.WHITE);
        paint2.setStyle(Paint.Style.FILL);
        TextPaint tp = new TextPaint();
        tp.setColor(Color.BLACK);
        tp.setTextSize(22);

        tp.setTextAlign(Paint.Align.LEFT);
        tp.setAntiAlias(true);
        if (bold) {
            tp.setTypeface(Typeface.create("Times New Roman", Typeface.BOLD));

        }

        StaticLayout Key = new StaticLayout(key, tp, 580, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);

        Bitmap image = Bitmap.createBitmap(600, Key.getLineCount() * 30, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawPaint(paint2);
        Key.draw(canvas);
        return image;
    }

    @ReactMethod
    public void ConnectPrinter(String ip, Promise promise) {
        try {
            Connect(ip);
            mPrinter.Beep(255, 255);
            prepareCanvas();
            promise.resolve("success");
        } catch (Exception e) {
            mPrinter.Disconnect();
            promise.reject(e);
        }
    }

    @ReactMethod
    public void cutPaper() {
        mPrinter.PaperFeed(200);
        mPrinter.CutPaper(0);
    }

    @ReactMethod
    public void printImage() {
        Bitmap image;
        image = createLogo2();
        mPrinter.PrintImage(image);
    }

    @ReactMethod
    public void disconnect() {
        mPrinter.Disconnect();
    }

    private void prepareCanvas() {
        fillPaint.setColor(Color.WHITE);
        fillPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(25);

    }

    @ReactMethod
    public void createNewLine() {
        listBitmap = new ListBitmapPrint();
    }

    @ReactMethod
    public void printText(String text, Integer fontSize,  Float widthProportion, Integer x, Integer y,Boolean isBold,
            Integer alignment) {
        Layout.Alignment layout;
        textPaint.setTextSize((float) fontSize);
        switch (alignment) {
            case 1:
                layout = Layout.Alignment.ALIGN_CENTER;
                break;
            case 2:
                layout = Layout.Alignment.ALIGN_OPPOSITE;
                break;
            default:
                layout = Layout.Alignment.ALIGN_NORMAL;
                break;
        }

        if(isBold){
            textPaint.setTypeface(Typeface.create("Times New Roman", Typeface.BOLD));
        }

        StaticLayout myStaticLayout = new StaticLayout(text, textPaint, (int) ((float) defaultWidth * widthProportion),
                layout, 1, 0, false);
        int layoutWidth = (int) ((float) defaultWidth * widthProportion);
        Bitmap b = Bitmap.createBitmap(layoutWidth, (int) myStaticLayout.getHeight() + y, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.translate(x, y);
        myStaticLayout.draw(c);
        listBitmap.addBitmap(b);
    }

    @ReactMethod
    public void printBarcode(String text, Integer x, Integer y, Float widthProportion, Integer alignment) {
        int padding = 10;
        Layout.Alignment layout;
        switch (alignment) {
            case 1:
                layout = Layout.Alignment.ALIGN_CENTER;
                break;
            case 2:
                layout = Layout.Alignment.ALIGN_OPPOSITE;
                break;
            default:
                layout = Layout.Alignment.ALIGN_NORMAL;
                break;
        }
        try {
            Bitmap b2 = TextToImageEncode(text);
            int layoutWidth = (int) ((float) defaultWidth * widthProportion);
            Bitmap b = Bitmap.createBitmap(layoutWidth, (int) b2.getHeight() + y, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            c.translate(x, y);

            c.drawBitmap(b2, 0, 0, textPaint);
            listBitmap.addBitmap(b);
        } catch (Exception e) {
            // TODO: handle exception
        }

    }

    @ReactMethod
    public void printLine() {
        float maxHeight = listBitmap.getMaxHeight();
        Bitmap b = Bitmap.createBitmap(defaultWidth, (int) maxHeight + 5, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.drawPaint(fillPaint);
        for(int i  = 0 ; i < listBitmap.getLength() ; i++) {
            c.drawBitmap(listBitmap.getBitmap(i) ,0,0 ,textPaint);
            if(i<listBitmap.getLength()-1){
                c.translate(listBitmap.getBitmap(i).getWidth(), 0);
            }
        }
        mPrinter.PrintImage(b);
    }

    @ReactMethod
    public void printDiagonal(Integer strokeWidth){
        Paint p = new Paint();
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(strokeWidth);
        Bitmap b = Bitmap.createBitmap(defaultWidth, 20, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.drawPaint(fillPaint);
        c.drawLine((float)0,(float)0,(float)defaultWidth,(float) 0, p);
        mPrinter.PrintImage(b);
    }

    @ReactMethod
    public void printSpace(Integer height) {
        Bitmap b = Bitmap.createBitmap(defaultWidth, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);        
        c.drawPaint(fillPaint);
        mPrinter.PrintImage(b);
    }

    @ReactMethod
    public void printLogo(String suborderId) {
        try {
            Bitmap image = createLogo(suborderId);
            mPrinter.PrintImage(image);    
        } catch (Exception e) {
            //TODO: handle exception
        }
        
    }
}

class ListBitmapPrint {
    List<Bitmap> listBitmap;

    public ListBitmapPrint() {
        listBitmap = new ArrayList<Bitmap>();
    };

    public List<Bitmap> getListBitmap() {
        return listBitmap;
    }

    public float getMaxHeight() {
        float maxHeight = 0;
        for (Bitmap sl : listBitmap) {
            float height = sl.getHeight();
            if (height > maxHeight) {
                maxHeight = height;
            }
        }
        return maxHeight;
    }

    public void addBitmap(Bitmap sl) {
        this.listBitmap.add(sl);
    }

    public Bitmap getBitmap(Integer idx) {
        return this.listBitmap.get(idx);
    }

    public float getHeighBitmap(Integer idx) {
        return this.listBitmap.get(idx).getHeight();
    }

    public int getWidthBitmap(Integer idx) {
        return this.listBitmap.get(idx).getWidth();
    }

    public int getLength(){
        return listBitmap.size();
    }
}
