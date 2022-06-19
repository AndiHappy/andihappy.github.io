package code;

import java.util.Arrays;

public class Code {
    public static void main(String[] args) {
        System.out.println("hello");
        // System.out.println(findMedianSortedArrays(new int[] { 1, 3 }, new int[] { 2
        // }));
        System.out.println(longestPalindrome_2("aaaa"));
    }

    public int reverse_1(int x) {
        boolean flag = x > 0;
        x = Math.abs(x);
        int result = 0, pre = 0;
        while (x != 0) {
            result = pre * 10 + x % 10;
            if ((result - x % 10) / 10 != pre)
                return 0;
            pre = result;
            x = x / 10;
        }
        return flag ? result : -result;
    }

    public static String longestPalindrome_2(String s) {
        if (s == null || s.length() < 2)
            return s;
        int from = 0, to = 0;
        boolean[][] dp = new boolean[s.length()][s.length()];

        for (int i = 0; i < s.length(); i++)
            dp[i][i] = true;
        for (int i = s.length() - 2; i >= 0; i--) {
            for (int j = i + 1; j < s.length(); j++) {
                if (s.charAt(i) == s.charAt(j) &&
                        ((j - i == 1) ||
                                (i < s.length() - 1 && j > 0 && dp[i + 1][j - 1]))) {
                    dp[i][j] = true;
                    if (j - i > (to - from)) {
                        from = i;
                        to = j;
                    }
                } else {
                    dp[i][j] = false;
                }
            }

        }
        return s.substring(from, to + 1);
    }

    public static double findMedianSortedArrays(int[] nums1, int[] nums2) {
        int m = nums1.length, n = nums2.length;
        int l = (m + n + 1) / 2;
        int r = (m + m + 2) / 2;
        System.out.println(getKth(nums1, 0, nums2, 0, l));
        System.out.println(getKth(nums1, 0, nums2, 0, r));
        return (getKth(nums1, 0, nums2, 0, l) + getKth(nums1, 0, nums2, 0, r)) / 2.0;
    }

    public static double getKth(int[] a, int astart, int[] b, int bstart, int k) {
        if (astart >= a.length)
            return b[bstart + k - 1];
        if (bstart >= b.length)
            return a[astart + k - 1];
        if (k == 1)
            return Math.min(a[astart], b[bstart]);

        int amid = Integer.MAX_VALUE, bmid = Integer.MAX_VALUE;
        if (astart + k / 2 - 1 < a.length)
            amid = a[astart + k / 2 - 1];
        if (bstart + k / 2 - 1 < b.length)
            bmid = b[bstart + k / 2 - 1];
        if (amid < bmid) {
            return getKth(a, astart + k / 2, b, bstart, k - k / 2);
        } else {
            return getKth(a, astart, b, bstart + k / 2, k - k / 2);
        }
    }

    // Median of Two Sorted Arrays
    public static double findMedianSortedArrays_1(int[] A, int[] B) {
        int m = A.length, n = B.length;
        int l = (m + n + 1) / 2, r = (m + n + 2) / 2;
        System.out.println(Arrays.toString(A) + " 0 " + Arrays.toString(B) + " 0 " + l);
        double lmid = getkth_1(A, 0, B, 0, l);
        System.out.println(Arrays.toString(A) + " 0 " + Arrays.toString(B) + " 0 " + r);
        double rmid = getkth_1(A, 0, B, 0, r);
        return (lmid + rmid) / 2.0;
    }

    // { 1, 3, 5, 7, 9 }, { 2, 4, 6, 8, 10 } ; k=5
    // aindex = 5/2-1 = 1, bindx = 5/2-1 = 1
    // amid = 3 ,bmid = 4
    // getKth({ 1, 3, 5, 7, 9 },2 { 2, 4, 6, 8, 10 },0, 3 )
    // aindex = 2+ 3/2-1 = 2, bindx = 0+3/2-1 = 0
    // amid = 5 ,bmid = 2
    // getKth({ 1, 3, 5, 7, 9 },2 { 2, 4, 6, 8, 10 },1, 2)
    // aindex = 2+ 2/2-1 = 2, bindx = 1+2/2-1 = 1
    // amid = 5 ,bmid = 4
    // getKth({ 1, 3, 5, 7, 9 },2 { 2, 4, 6, 8, 10 },2, 1)
    // return Math.min(A[2], B[2]) = 5
    public static double getkth_1(int[] A, int aStart, int[] B, int bStart, int k) {
        if (aStart > A.length - 1)
            return B[bStart + k - 1];
        if (bStart > B.length - 1)
            return A[aStart + k - 1];
        if (k == 1)
            return Math.min(A[aStart], B[bStart]);

        int aMid = Integer.MAX_VALUE, bMid = Integer.MAX_VALUE;
        if (aStart + k / 2 - 1 < A.length)
            aMid = A[aStart + k / 2 - 1];
        if (bStart + k / 2 - 1 < B.length)
            bMid = B[bStart + k / 2 - 1];
        if (aMid < bMid) {
            System.out
                    .println(Arrays.toString(A) + " " + (aStart + k / 2) + " " + Arrays.toString(B) + " " + bStart + " "
                            + (k - k / 2));
            return getkth_1(A, aStart + k / 2, B, bStart, k - k / 2);
        } else {
            System.out
                    .println(Arrays.toString(A) + " " + aStart + " " + Arrays.toString(B) + " " + (bStart + k / 2) + " "
                            + (k - k / 2));
            return getkth_1(A, aStart, B, bStart + k / 2, k - k / 2);
        }
    }

    // Longest Palindromic Substring
    // dp 解法
    // dp[i][j] from i to j is palindromic substring
    // dp[i][i]=true
    // dp[i][j]= 【charAt(i) == charAt(j) && j-i == 1 】|| 【charAt(i) == charAt(j) &&
    // dp[i+1][j-1] && i,j 不越界】
    public static String longestPalindrome(String a) {

        if (a == null || a.length() < 2)
            return a;
        boolean[][] dp = new boolean[a.length()][a.length()];
        int start = 0, end = 0;
        for (int i = 0; i < a.length(); i++)
            dp[i][i] = true;
        for (int i = a.length() - 2; i >= 0; i--) {
            for (int j = i + 1; j < a.length(); j++) {
                if (a.charAt(i) == a.charAt(j)
                        && (j - i == 1
                                || (i < a.length() - 1 && j > 0 && dp[i + 1][j - 1]))) {
                    dp[i][j] = true;
                    if (j - i > (end - start)) {
                        start = i;
                        end = j;
                    }
                } else {
                    dp[i][j] = false;
                }
            }
        }
        return a.substring(start, end + 1);
    }

    // Reverse Integer
    public int reverse(int x) {
        // add a tmp vara to store the pre value
        int res = 0, sum = 0;
        while (x != 0) {
            int tail = x % 10;
            sum = sum * 10 + tail;
            if (((sum - tail) / 10) != res)
                return 0;
            res = sum;
            x = x / 10;
        }
        return res;
    }

    // String to integer
    public static int myAtoi(String s) {
        // exception judge
        if (s == null || s.length() == 0)
            return 0;
        char[] chararray = s.toCharArray();
        // The function first discards as many whitespace characters as necessary until
        // the first non-whitespace character is found
        int result = 0, flag = 1, cur = 0;
        while (cur < s.length() && chararray[cur] == ' ')
            cur++;

        // starting from this character takes an optional initial plus or minus sign
        if (cur < s.length() && chararray[cur] == '-') {
            flag = -1;
            cur++;
        } else if (cur < s.length() && chararray[cur] == '+') {
            flag = 1;
            cur++;
        }

        // followed by as many numerical digits as possible, and interprets them as a
        // numerical value
        while (cur < s.length() && Character.isDigit(chararray[cur])) {
            int tmp = chararray[cur] - '0';
            // Assume we are dealing with an environment that could only store integers with
            // in the 32-bit signed integer range: [−231, 231 − 1]. If the numerical value
            // is out
            // of the range of representable values, 231 − 1 or −231 is returned
            if ((Integer.MAX_VALUE - tmp) / 10 < result) {
                return flag > 0 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            } else {
                result = result * 10 + tmp;
            }
            cur++;
        }
        return flag * result;
    }

}
