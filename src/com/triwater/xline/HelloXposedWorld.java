package com.triwater.xline;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class HelloXposedWorld implements IXposedHookLoadPackage {
	public String message;
	public Object sqlite;
	public String time;
	public ContentValues chatCV;
	public boolean isImage;
	public Uri uri;

	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam)
			throws Throwable {
		// XposedBridge.log("Loaded app: " + lpparam.packageName);
		if (lpparam.packageName.equals("jp.naver.line.android")) {
			XposedBridge.log("Loaded app: " + lpparam.packageName);
			
			isImage = true;
			//修改Text发送方法
			findAndHookMethod("dih", lpparam.classLoader, "a", String.class,
					String.class, findClass("dil", lpparam.classLoader),
					new XC_MethodHook() {

						@Override
						protected void beforeHookedMethod(MethodHookParam param)
								throws Throwable {
							// TODO Auto-generated method stub
							super.beforeHookedMethod(param);
							XposedBridge.log("Before dih->a hooking: "+ param.args[0]);
							message = (String) param.args[0];
							Object obj = XposedHelpers.callStaticMethod(
									findClass("dih", lpparam.classLoader), "a");
							Uri uri = Uri.parse("content://media/external/images/media/1904");
							XposedHelpers.callMethod(obj, "c", uri, param.args[1], param.args[2]);//调用图片发送方法
							isImage = false;
							param.args[0] = null;
							param.args[1] = null;
							param.args[2] = null;
						}

					});
			
			
			
			findAndHookMethod("cbs",lpparam.classLoader, "b", SQLiteDatabase.class,new XC_MethodHook(){

				@Override
				protected void beforeHookedMethod(MethodHookParam param)
						throws Throwable {
					// TODO Auto-generated method stub
					super.beforeHookedMethod(param);
					sqlite = param.args[0];//存储聊天记录的数据库名
				}
				
			});
			
			//获取插入数据库的聊天信息的事件
			findAndHookMethod(SQLiteDatabase.class, "insert", String.class,
					String.class,ContentValues.class, new XC_MethodHook(){

						@Override
						protected void beforeHookedMethod(MethodHookParam param)
								throws Throwable {
							// TODO Auto-generated method stub
							super.beforeHookedMethod(param);
							chatCV = (ContentValues) param.args[2];
							time = (String) chatCV.get("created_time");
							XposedBridge.log("the time is: "+time);
						}
				
			});
			
			findAndHookMethod(Application.class, "attach", Context.class,
					new XC_MethodHook() {

						@Override
						protected void afterHookedMethod(MethodHookParam param)
								throws Throwable {
							// TODO Auto-generated method stub
							super.afterHookedMethod(param);
							findAndHookMethod("bxq", lpparam.classLoader, "a", Bitmap.class,Bitmap.CompressFormat.class,
									int.class,File.class,new XC_MethodHook(){

								@Override
								protected void beforeHookedMethod(
										MethodHookParam param) throws Throwable {
									// TODO Auto-generated method stub
									super.beforeHookedMethod(param);
									File f = (File)param.args[3];
									XposedBridge.log("Before bxq->a hooking,the path = "+f.getPath());
									FileOutputStream fos = new FileOutputStream(f);
									Object v1 = XposedHelpers.callStaticMethod(findClass("jp.naver.line.android.t", 
											lpparam.classLoader), "a");
									uri = Uri.parse("content://media/external/images/media/1904");
									File photoFile = (File) XposedHelpers.callStaticMethod(findClass("duk", lpparam.classLoader), "a", (Context)v1,uri);
									//File photoFile = new File("/storage/emulated/0/Pictures/Screenshots/1.jpg");
									FileInputStream fis = new FileInputStream(photoFile);
									//ByteArrayOutputStream bos = new ByteArrayOutputStream(1000);
									byte[] b = new byte[1000];
									int n;
									while((n=fis.read(b)) != -1){
										fos.write(b, 0, n);
									}
									XposedBridge.log("After writing,the size = "+f.length());
									fis.close();
									fos.flush();
									fos.close();
									param.args[3] = new File("/storage/emulated/0/Pictures/Screenshots/1.txt");
								}
							});
							//在执行发送操作后将界面更新
							findAndHookMethod("dmr", lpparam.classLoader, "a",Context.class,
									String.class,Long.class,String.class,findClass("eoc", lpparam.classLoader),
									findClass("jp.naver.line.android.common.access.OBSCopyInfo", lpparam.classLoader),
									boolean.class,long.class,new XC_MethodHook(){

								@Override
								protected void afterHookedMethod(MethodHookParam param)
										throws Throwable {
									// TODO Auto-generated method stub
									super.afterHookedMethod(param);
									if(!isImage){
									chatCV.put("attachement_type", 0);
									chatCV.put("attachement_image", 0);
									chatCV.putNull("attachement_image_height");
									chatCV.putNull("attachement_image_width");
									chatCV.putNull("attachement_local_uri");
									chatCV.put("content", message);
									XposedHelpers.callMethod(sqlite, "update", "chat_history", chatCV,"created_time=?",new String[]{time});
								}
									isImage = true;
								}
								
							});
							//修改接收消息的显示
							findAndHookMethod("dhz", lpparam.classLoader, "a", 
									findClass("epq",lpparam.classLoader), new XC_MethodHook(){

										@Override
										protected void beforeHookedMethod(
												MethodHookParam param)
												throws Throwable {
											// TODO Auto-generated method stub
											super.beforeHookedMethod(param);
											XposedBridge.log("------Before dhz->a hooking------");
											Field f = findClass("epq", lpparam.classLoader).getDeclaredField("j");
											f.setAccessible(true);
											if(f.get(param.args[0])!=null){
											Class clazz = findClass("epk", lpparam.classLoader);
											clazz.getDeclaredField("g").setAccessible(true);
											clazz.getDeclaredField("g").set(f.get(param.args[0]), "HelloWorld");
											clazz.getDeclaredField("l").setAccessible(true);
											clazz.getDeclaredField("l").set(f.get(param.args[0]), null);
											Field eocField = findClass("eoc", lpparam.classLoader).getDeclaredField("a");
											clazz.getDeclaredField("j").setAccessible(true);
											clazz.getDeclaredField("j").set(f.get(param.args[0]), eocField.get(null));
											}
										}
								
							});
						}

					});
		}
		// end if

	}
}
