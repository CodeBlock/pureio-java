package me.elrod.pureio;

import java.io.*;
import java.util.ArrayList;

/**
 * A compatibility interpreter for our free IO monads to make them do things in
 * a way that a typical Java environment might expect.
 *
 * You could implement your own to do cooler, better, things.
 */
public class UnsafePerformIO {
    final static BufferedReader in =
        new BufferedReader(new InputStreamReader(System.in));

    public static <A> A unsafePerformIO(PureConsoleIO<A> t) {
        return t.cata(
            a -> a,
            a -> a.cata(
                (s, tt) -> {
                    System.out.println(s);
                    return unsafePerformIO(tt);
                },
                f       -> {
                    try {
                        String s = in.readLine();
                        return unsafePerformIO(f.apply(s));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                (ec, tt) -> {
                    System.exit(ec);
                    return unsafePerformIO(tt);
                }));
    }

    /**
     * Same thing as {@link unsafePerformIO} except for {@link PureConsoleIOT}-style
     * trampolining.
     */
    public static <A> PureConsoleIOT<A> unsafePerformIOT(TerminalOperation<PureConsoleIOT<A>> t) {
        return t.cata(
            (s, tt) -> {
                System.out.println(s);
                return tt;
            },
            f       -> {
                try {
                    String s = in.readLine();
                    return f.apply(s);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            },
            (ec, tt) -> {
                System.exit(ec);
                return tt;
            });
    }

    public static <A> A unsafePerformFileIO(PureFileIO<A> t) {
        return t.cata(
            a -> a,
            a -> a.cata(
                (filename, f) -> {
                    try {
                        ArrayList<String> al = new ArrayList<String>();
                        BufferedReader in = new BufferedReader(new FileReader(filename));
                        while (in.ready()) {
                            al.add(in.readLine());
                        }
                        in.close();
                        return unsafePerformFileIO(f.apply(LinkedList.fromArray(al.toArray(new String[0]))));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                (data, f) -> {
                    try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(data.run1(), true)))) {
                            out.print(data.run2());
                            return unsafePerformFileIO(f);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }));
    }
}
