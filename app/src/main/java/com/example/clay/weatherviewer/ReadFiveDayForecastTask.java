// ReadFiveDayForecastTask.java
// Read the next five daily forecasts in a background thread.
package com.example.clay.weatherviewer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;

class ReadFiveDayForecastTask extends AsyncTask<Object, Object, String> 
{
   private static final String TAG = "ReadFiveDayForecastTask";   
   
   private String zipcodeString;
   private FiveDayForecastLoadedListener weatherFiveDayForecastListener;
   private Resources resources;
   private DailyForecast[] forecasts;
   private static final int NUMBER_OF_DAYS = 5;
   
   // interface for receiver of weather information
   public interface FiveDayForecastLoadedListener 
   {
      public void onForecastLoaded(DailyForecast[] forecasts);
   } // end interface FiveDayForecastLoadedListener
   
   // creates a new ReadForecastTask
   public ReadFiveDayForecastTask(String zipcodeString, 
      FiveDayForecastLoadedListener listener, Context context)
   {
      this.zipcodeString = zipcodeString;
      this.weatherFiveDayForecastListener = listener;
      this.resources = context.getResources();
      this.forecasts = new DailyForecast[NUMBER_OF_DAYS];
   } // end constructor ReadFiveDayForecastTask
      
   @Override
   protected String doInBackground(Object... params) 
   {
      // the url for the WorldWeatherOnline JSON service
      try 
      {
         URL webServiceURL = new URL(resources.getString(
                 R.string.pre_zipcode_url) + zipcodeString
                 + zipcodeString  +
             "&format=json&num_of_days=5&fx=yes&cc=no&mca=no&fx24=no"
                 +"&includelocation=no&show_comments=no&tp=24"
                 +"&key=400a485fad960489458a8cea3e1fc");
         // create a stream Reader from the WorldWeatherOnline url
         Reader forecastReader = new InputStreamReader(
            webServiceURL.openStream());
             
         // create a JsonReader from the Reader
         JsonReader forecastJsonReader = new JsonReader(forecastReader);
             
         forecastJsonReader.beginObject(); // read the next Object

         // get the next name
         String name = forecastJsonReader.nextName();
            
         // if its the name expected for hourly forecast information
         if (name.equals(resources.getString(R.string.forecast_list))) 
         {
            forecastJsonReader.beginObject();

            while (forecastJsonReader.hasNext()
                    && !forecastJsonReader.nextName()
                        .equals(resources.getString(R.string.weather_tag)))
                forecastJsonReader.skipValue(); // skip today's forecast
             forecastJsonReader.beginArray();

            // read the next five daily forecasts
            for (int i = 0; i < NUMBER_OF_DAYS; i++)
            {    
               // start reading the next object
               forecastJsonReader.beginObject(); 

               // if there is more data
               if (forecastJsonReader.hasNext())
               {
                  //read the next forecast
                  forecasts[i] = readDailyForecast(forecastJsonReader); 
               } // end if
            } // end for
         } // end if
              
         forecastJsonReader.close(); // close the JsonReader
         
      } // end try 
      catch (MalformedURLException e) 
      {
         Log.v(TAG, e.toString());
      } // end catch
      catch (NotFoundException e) 
      {
    	  Log.v(TAG, e.toString());
      } // end catch 
      catch (IOException e) 
      {
    	  Log.v(TAG, e.toString());
      } // end catch
      return null;
   } // end method doInBackground
   
   // read a single daily forecast
   private DailyForecast readDailyForecast(JsonReader forecastJsonReader)
   {
      // create array to store forecast information
      String[] dailyForecast = new String[4]; 
      Bitmap iconBitmap = null; // store the forecast's image
      
      try 
      {
         // while there is a next element in the current object
         while (forecastJsonReader.hasNext()) 
         {
            String name = forecastJsonReader.nextName(); // read next name

            if (name.equals(resources.getString(R.string.date)))
            {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                Date date = null;
                try {
                    date = format.parse(forecastJsonReader.nextString());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
                calendar.setTime(date);
                dailyForecast[DailyForecast.DAY_INDEX] =
                       getDayName(calendar.get(Calendar.DAY_OF_WEEK));
            } // end if
            else if (name.equals(resources.getString(R.string.hourly))) {
                forecastJsonReader.beginArray();
                forecastJsonReader.beginObject();
                while (forecastJsonReader.hasNext()) {
                    name = forecastJsonReader.nextName();
                    if (name.equals(resources.getString(R.string.day_prediction))) {
                        forecastJsonReader.beginArray();
                        forecastJsonReader.beginObject();
                        name = forecastJsonReader.nextName();
                        dailyForecast[DailyForecast.PREDICTION_INDEX] =
                                forecastJsonReader.nextString();
                        forecastJsonReader.endObject();
                        forecastJsonReader.endArray();
                    } else if (name.equals(resources.getString(R.string.day_icon))) {
                        // read the icon name
                        forecastJsonReader.beginArray();
                        forecastJsonReader.beginObject();
                        forecastJsonReader.nextName();
                        iconBitmap = ReadForecastTask.getIconBitmap(
                                forecastJsonReader.nextString(), resources, 0);
                        forecastJsonReader.endObject();
                        forecastJsonReader.endArray();
                    } else {
                        forecastJsonReader.skipValue();
                    }
                }
                forecastJsonReader.endObject();
                forecastJsonReader.endArray();

            }
            else if (name.equals(resources.getString(R.string.high))) 
            {
               dailyForecast[DailyForecast.HIGH_TEMP_INDEX] = 
                  forecastJsonReader.nextString(); 
            } // end else if 
            else if (name.equals(resources.getString(R.string.low))) 
            {
               dailyForecast[DailyForecast.LOW_TEMP_INDEX] = 
                  forecastJsonReader.nextString(); 
            } // end else if
            else // there is an unexpected element
            {
               forecastJsonReader.skipValue(); // skip the next element
            } // end else
         } // end while
         forecastJsonReader.endObject();
      } // end try
      catch (IOException e) 
      {
         Log.e(TAG, e.toString());
      } // end catch
      
      return new DailyForecast(dailyForecast, iconBitmap);
   } // end method readDailyForecast

    private String getDayName(int dayNum) {
        switch (dayNum) {
            case Calendar.SUNDAY: return "Sunday";
            case Calendar.MONDAY: return "Monday";
            case Calendar.TUESDAY: return "Tuesday";
            case Calendar.WEDNESDAY: return "Wednesday";
            case Calendar.THURSDAY: return "Thursday";
            case Calendar.FRIDAY: return "Friday";
            case Calendar.SATURDAY: return "Saturday";
        }
        return "Who knows?";
    }

   // update the UI back on the main thread
   protected void onPostExecute(String forecastString) 
   {
      weatherFiveDayForecastListener.onForecastLoaded(forecasts);
   } // end method onPostExecute
} // end class ReadFiveDayForecastTask