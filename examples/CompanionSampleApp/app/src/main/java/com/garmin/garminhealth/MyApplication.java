package com.garmin.garminhealth;

import android.app.Application;

import com.facebook.stetho.Stetho;

/**
 * Copyright (c) 2017 Garmin International. All Rights Reserved.
 * <p></p>
 * This software is the confidential and proprietary information of
 * Garmin International.
 * You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement
 * you entered into with Garmin International.
 * <p></p>
 * Garmin International MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
 * SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. Garmin International SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 * <p></p>
 * Created by jacksoncol on 4/26/18.
 */
public class MyApplication extends Application
{
    public void onCreate()
    {
        super.onCreate();

        // Stetho lets you view database entries in development.
        Stetho.initializeWithDefaults(this);
    }
}