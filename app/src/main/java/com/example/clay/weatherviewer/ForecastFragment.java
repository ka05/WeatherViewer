// ForecastFragment.java
// An abstract class defining a Fragment capable of providing a zipcode.
package com.example.clay.weatherviewer;

import android.app.Fragment;

public abstract class ForecastFragment extends Fragment
{
   public abstract String getZipcode();
} // end class ForecastFragment
