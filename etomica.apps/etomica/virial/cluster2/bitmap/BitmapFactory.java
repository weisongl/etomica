package etomica.virial.cluster2.bitmap;

import etomica.virial.cluster2.bitmap.impl.BitmapOfBigInteger;
import etomica.virial.cluster2.bitmap.impl.BitmapOfLong;
import etomica.virial.cluster2.bitmap.impl.BitmapOfLongVector;

public class BitmapFactory {

  public static boolean useBigInteger = false;
  public static final Bitmap ONE = new BitmapOfLong(1, true);
  public static final Bitmap ZERO = new BitmapOfLong(1, false);
  public static final Bitmap EMPTY = new BitmapOfLong() {

    @Override
    public boolean equals(final Object other) {

      return other == this;
    }

    @Override
    public int compareTo(final Bitmap other) {

      if (this == other) {
        return 0;
      }
      return -1;
    }
  };

  public static final Bitmap getBitmap(final String bitmap) {

    if (bitmap.length() <= Bitmap.SZ_LONG) {
      return new BitmapOfLong(bitmap);
    } else if (!BitmapFactory.useBigInteger) {
      return new BitmapOfLongVector(bitmap);
    } else {
      return new BitmapOfBigInteger(bitmap);
    }
  }

  public static final Bitmap getBitmap(final int capacity, boolean isSet) {
    // return new BitmapOfLongVector(capacity, isSet);

    if (capacity <= Bitmap.SZ_LONG) {
      return new BitmapOfLong(capacity, isSet);
    } else if (!BitmapFactory.useBigInteger) {
      return new BitmapOfLongVector(capacity, isSet);
    } else {
      return new BitmapOfBigInteger(capacity, isSet);
    }
  }

  public static Bitmap upperTriangleBitmap(byte nodeCount, boolean isSet) {

    if (nodeCount == 1) {
      return BitmapFactory.ZERO;
    } else if (nodeCount > 1) {
      int numEdges = nodeCount * (nodeCount - 1) / 2;
      return BitmapFactory.getBitmap(numEdges, isSet);
    }
    return BitmapFactory.EMPTY;
  }
}