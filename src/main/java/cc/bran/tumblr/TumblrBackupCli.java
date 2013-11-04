package cc.bran.tumblr;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import cc.bran.tumblr.api.JumblrTumblrApi;
import cc.bran.tumblr.api.TumblrApi;
import cc.bran.tumblr.persistence.SqlitePostDb;
import cc.bran.tumblr.types.Post;

import com.google.common.collect.Iterables;
import com.tumblr.jumblr.JumblrClient;

/**
 * CLI program that backs up an entire Tumblr to a single database.
 * 
 * @author Brandon Pitman (brandon.pitman@gmail.com)
 */
public class TumblrBackupCli {

  private static final int POST_GROUPING_SIZE = 20;

  @SuppressWarnings("static-access")
  private static Options buildCommandLineOptions() {
    Options options = new Options();
    options.addOption(OptionBuilder.withArgName("tumblr-name").hasArg().isRequired()
            .withDescription("name of Tumblr to back up").withLongOpt("tumblr-name").create());
    options.addOption(OptionBuilder.withArgName("db-file").hasArg().isRequired()
            .withDescription("filename of database to back up into").withLongOpt("db-file")
            .create());
    options.addOption(OptionBuilder.withArgName("key-file").hasArg().isRequired()
            .withDescription("tumblr API key file").withLongOpt("key-file").create());
    return options;
  }

  public static JumblrClient jumblrClientFromKeyFile(String keyFile) throws IOException {
    Path path = FileSystems.getDefault().getPath(keyFile);
    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

    if (lines.size() != 4) {
      throw new IOException("bad keyfile format");
    }

    return new JumblrClient(lines.get(0), lines.get(1), lines.get(2), lines.get(3));
  }

  public static void main(String[] args) throws Exception {
    // Parse command line options.
    CommandLine commandLine = new PosixParser().parse(buildCommandLineOptions(), args);
    String tumblrName = commandLine.getOptionValue("tumblr-name");
    String dbFile = commandLine.getOptionValue("db-file");
    String keyFile = commandLine.getOptionValue("key-file");

    JumblrClient jumblrClient = jumblrClientFromKeyFile(keyFile);
    TumblrApi tumblrApi = new JumblrTumblrApi(jumblrClient);

    try (SqlitePostDb postDb = new SqlitePostDb(dbFile)) {
      for (List<Post> posts : Iterables.partition(tumblrApi.getAllPosts(tumblrName),
              POST_GROUPING_SIZE)) {
        postDb.put(posts);
      }
    }
  }
}
