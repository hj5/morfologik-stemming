package morfologik.tools;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import morfologik.stemming.DictionaryLookup;
import morfologik.stemming.DictionaryMetadataBuilder;

import org.junit.Test;

import com.carrotsearch.hppc.ByteArrayList;
import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.Name;
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;
import com.google.common.base.Charsets;

public class MorphEncoder2Test extends RandomizedTest {
    private final MorphEncoder2.IEncoder coder;

    public MorphEncoder2Test(@Name("coder") MorphEncoder2.IEncoder coder)
    {
        this.coder = coder;
    }

    @ParametersFactory
    public static List<Object[]> testFactory() {
        return Arrays.asList($$(
            $(new MorphEncoder2.TrimSuffixEncoder()),
            $(new MorphEncoder2.TrimPrefixAndSuffixEncoder()),
            $(new MorphEncoder2.TrimInfixAndSuffixEncoder())
        ));
    }

    @Test
    public void testEncodeSuffixOnRandomSequences() {
        for (int i = 0; i < 10000; i++) {
            assertRoundtripEncode(
                randomAsciiOfLengthBetween(0, 500),
                randomAsciiOfLengthBetween(0, 500));
        }
    }

    @Test
    public void testEncodeSamples() {
        assertRoundtripEncode("", "");
        assertRoundtripEncode("abc", "ab");
        assertRoundtripEncode("abc", "abx");
        assertRoundtripEncode("ab", "abc");
        assertRoundtripEncode("xabc", "abc");
        assertRoundtripEncode("axbc", "abc");
        assertRoundtripEncode("axybc", "abc");
        assertRoundtripEncode("axybc", "abc");
        assertRoundtripEncode("azbc", "abcxy");

        assertRoundtripEncode("Niemcami", "Niemiec");
        assertRoundtripEncode("Niemiec", "Niemcami");
    }

    private void assertRoundtripEncode(String srcString, String dstString)
    {
        ByteArrayList src = ByteArrayList.from(srcString.getBytes(UTF8));
        ByteArrayList dst = ByteArrayList.from(dstString.getBytes(UTF8));
        ByteArrayList encoded = ByteArrayList.newInstance();
        ByteArrayList decoded = ByteArrayList.newInstance();

        coder.encode(src, dst, encoded);
        coder.decode(src, encoded, decoded);

        if (!dst.equals(decoded)) {
            System.out.println("src: " + new String(src.toArray(), Charsets.UTF_8));
            System.out.println("dst: " + new String(dst.toArray(), Charsets.UTF_8));
            System.out.println("enc: " + new String(encoded.toArray(), Charsets.UTF_8));
            System.out.println("dec: " + new String(decoded.toArray(), Charsets.UTF_8));
        }
        
        assertEquals(dst, decoded);
        
        // TODO: add DictionaryLookup.decodeBaseForm decoding testing
        DictionaryMetadataBuilder builder = new DictionaryMetadataBuilder();
        builder.encoding(Charsets.UTF_8);
        builder.useInfixes(coder instanceof MorphEncoder2.TrimInfixAndSuffixEncoder);
        builder.usePrefixes(coder instanceof MorphEncoder2.TrimPrefixAndSuffixEncoder);
            
        ByteBuffer bb = DictionaryLookup.decodeBaseForm(
            ByteBuffer.allocate(0),
            encoded.toArray(), 
            encoded.size(), 
            ByteBuffer.wrap(src.toArray()), builder.build());
        
        ByteArrayList decoded2 = ByteArrayList.newInstance();
        bb.flip();
        while (bb.hasRemaining()) decoded2.add(bb.get());

        if (!dst.equals(decoded2)) {
            System.out.println("DictionaryLookup.decodeBaseForm incorrect, coder: " + coder);
            System.out.println("src : " + new String(src.toArray(), Charsets.UTF_8));
            System.out.println("dst : " + new String(dst.toArray(), Charsets.UTF_8));
            System.out.println("enc : " + new String(encoded.toArray(), Charsets.UTF_8));
            System.out.println("dec : " + new String(decoded.toArray(), Charsets.UTF_8));
            System.out.println("dec2: " + new String(decoded2.toArray(), Charsets.UTF_8));
        }

        assertEquals(dst, decoded2);       
    }

    public static void main(String [] args) {
        /*
        ByteArrayList src = ByteArrayList.from("1a2345678Y".getBytes(UTF_8));
        ByteArrayList dst = ByteArrayList.from("123456789X".getBytes(UTF_8));

        MorphEncoder2.IEncoder coder = new MorphEncoder2.TrimInfixAndSuffixEncoder();
        ByteArrayList out = coder.encode(src, dst, ByteArrayList.newInstance());
        System.out.println(new String(out.toArray()));
        System.out.println(new String(coder.decode(src, out, ByteArrayList.newInstance()).toArray()));
        */
    }    
}
