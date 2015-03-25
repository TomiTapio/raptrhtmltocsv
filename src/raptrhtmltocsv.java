import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* How to use: save your Raptr game collection pages as html. I have 29 pages, so 29 html files.
command line: java raptrhtmltocsv "filename of html"
it appends to My Game Collection.csv
then you open csv file in spreadsheet program. Perhaps LibreOffice. Save as a better format, like Excel 97.
*/
public class raptrhtmltocsv {
    public static void main(String[] args) {
        System.out.println("TomiTapio's Raptr game collection html to CSV exporter. separator=;");
        String infilename = args[0];
        //String outfilename = new StringBuilder().append(args[0]).append(".csv").toString();
        String outfilename = "My Game Collection.csv";

        InputStream ins = null; // raw byte-stream
        Reader r = null; // cooked reader
        BufferedReader br = null; // buffered for readLine()

        try	{
            File f = new File(outfilename);
            FileWriter writer = new FileWriter(outfilename, true/*append*/);
            if (f.exists() && f.length() > 10) {
                //if desti file exists, append.
                System.out.println("appending to "+ outfilename);
            } else {
                //if not exist, append header row to it.
                System.out.println("creating file "+ outfilename + " and writing header row");
                writer.append("Game;");
                writer.append("Platform;");
                writer.append("Hours;");
                writer.append("My Rating;");
                writer.append("Favorite;");
                writer.append("AT unlocked;");
                writer.append("AT max;");
                writer.append("Raptr rank;");
                writer.append('\n');
            }
            //cant f.close(), no such thing

            String currLine;
            ins = new FileInputStream(infilename);
            r = new InputStreamReader(ins, "UTF-8"); // leave charset out for default
            br = new BufferedReader(r);
            System.out.println("reading from file: "+infilename);

            String curr_game_fav = "-";
            String curr_game_name = "-";
            String curr_game_platform = "-";
            String curr_game_rank = "-";
            Integer curr_game_ratingstars = new Integer(0); //src: 0.0? to 5.0
            Integer curr_game_hours = new Integer(0);
            String curr_game_achgotten = ""; //can be ""
            String curr_game_achmax = ""; //can be ""

            //look for favorite: <div class="game-favorite">   <span class="toggle-button" data-params=____ data-done="yes" data-force-refresh="">
            String regex_favorite = "<div.+game-favorite.+span class=\"toggle-button\" data-params=.+ data-done=\"(.+)\" data-force";
            Pattern pattern_fav = Pattern.compile(regex_favorite);

            //look for GAME NAME:  <a href="http://raptr.com/game/XBLA/A_World_of_Keflings" class="game-title">A World of Keflings (XBLA)</a>
            String regex_name = "<a href.+raptr.+class..game-title..(.*)</a>";
            Pattern pattern_name = Pattern.compile(regex_name);
            //breaks on:  </button></div><div class="user-info-playing"><span class="playing-label ">Last Played</span><div class="game-boxart"><a href="http://raptr.com/game/Eldritch">      <img src="thomaskorat%27s%20Game%20Collection%2028%20-%20Raptr_files/boxart-small.jpg" class="" height="34" width="34"></a></div><div class="played-game-info"><a class="game-title" href="http://raptr.com/game/Eldritch">Eldritch</a><span class="time-rank"><span class="icon time"></span><span class="playing-time">209 days ago</span><span class="icon rank"></span><span class="RANKLEVEL5">Elite</span></span></div></div></div>
            //if matches badline class="played-game-info", skip line (currline="").
            String regex_bad = "class=.played-game-info";
            Pattern pattern_bad = Pattern.compile(regex_bad);



            //look for verbal rank: <li class="game-rank">        <span class="RANKLEVEL5">Elite</span>
            String regex_rank = "<li class=.game-rank.>.*<span class=.RANKLEVEL(.).>(.+)</span>";
            Pattern pattern_rank = Pattern.compile(regex_rank);

            //look for user rating stars: <li style="width: 51px;" class="current rating-control-user" data-rating="3.00">        </li><li>
            String regex_rating = "<li.+class..current.rating-control-user..data-rating=\"(.).00\">.*</li>"; //it's integers, cant have 3.50
            Pattern pattern_rating = Pattern.compile(regex_rating);

            //look for hours played: <li class="game-hours">      <span class="icon"></span> <strong>7 hrs</strong></li>
            // if "x mins" or "0 secs" then zero.
            String regex_hours = "<li class=.game-hours.+<span class.+<strong>(\\d+).(hrs)</strong>.*</li>";
            Pattern pattern_hours = Pattern.compile(regex_hours);

            //look for achs: <li class="game-achievements"> <a href=__ class="achievement-link"> LINEBREAK_BLEH <span class="icon"></span> <strong>0</strong> of 48 </a></li>
            // <span class="icon"></span> <strong>18</strong> of 50
            String regex_ach = "<span class=\"icon\"></span> .+strong>(\\d+)</strong> of (\\d+)";
            Pattern pattern_ach = Pattern.compile(regex_ach);

            //read each line, looking for the target strings for data extraction
            while ((currLine = br.readLine()) != null) {
                try {
                    //does currline match A,B,C,D,E,F,G condition
                    if (currLine.length() < 21) //speedup
                        continue;

                    //look for favorite
                    Matcher matcher_fav = pattern_fav.matcher(currLine);
                    if (matcher_fav.find()) {
                        //new game section begins with FAV. Output previous game if have one.
                        if (curr_game_name.length() > 2) {
                            writer.append(curr_game_name + ";");
                            writer.append(curr_game_platform + ";");
                            writer.append(curr_game_hours.toString() + ";");
                            writer.append(curr_game_ratingstars.toString() + ";");
                            writer.append(curr_game_fav + ";");
                            writer.append(curr_game_achgotten + ";");
                            writer.append(curr_game_achmax + ";");
                            writer.append(curr_game_rank + ";");
                            writer.append('\n');

                            // reset curr game knowledge
                            curr_game_fav = "-";
                            curr_game_name = "-";
                            curr_game_platform = "-";
                            curr_game_rank = "-";
                            curr_game_ratingstars = new Integer(0); //src: 0.0? to 5.0
                            curr_game_hours = new Integer(0);
                            curr_game_achgotten = ""; //can be ""
                            curr_game_achmax = ""; //can be ""
                        }
                        //store currline's findings into var
                        curr_game_fav = matcher_fav.group(1); // "yes" or "no"
                    }

                    //before look for GAME NAME, look for superlong badline about other user playing info
                    Matcher matcher_bad = pattern_bad.matcher(currLine);
                    if (matcher_bad.find()) { //store currline's findings into var
                        //currLine = "";
                        //skip this line
                        continue;
                    }
                    //look for GAME NAME
                    Matcher matcher_name = pattern_name.matcher(currLine);
                    if (matcher_name.find()) { //store currline's findings into var
                        curr_game_name = matcher_name.group(1); //incl platform in parentheses
                        curr_game_name = curr_game_name.replace("&amp;", "&"); // "Beyond Good & Evil"
                        curr_game_name = curr_game_name.replace(";", "");
                        curr_game_name = curr_game_name.replace(",", ""); //"warhammer 40,000" broke CSV import
                        //extract platform string
                        String[] foo = curr_game_name.split("[(.+).\\((.+)\\)]");
                        //System.out.println(foo[0]+"_"+foo[1]);
                        curr_game_platform = foo[1];
                        System.out.println("found game: "+curr_game_name + ", platform: "+curr_game_platform);
                    }

                    //rank
                    Matcher matcher_rank = pattern_rank.matcher(currLine);
                    if (matcher_rank.find()) { //store currline's findings into var
                        //xxx (1) has numeric, not gonna export.
                        curr_game_rank = matcher_rank.group(2);
                    }

                    //rating stars
                    Matcher matcher_rating = pattern_rating.matcher(currLine);
                    if (matcher_rating.find()) { //store currline's findings into var
                        String ra = matcher_rating.group(1);
                        curr_game_ratingstars = new Integer(ra);
                    }

                    //hours played
                    Matcher matcher_hours = pattern_hours.matcher(currLine);
                    if (matcher_hours.find()) { //store currline's findings into var
                        String h = matcher_hours.group(1);
                        curr_game_hours = new Integer(h);
                    }

                    // ach have/max
                    Matcher matcher_ach = pattern_ach.matcher(currLine);
                    if (matcher_ach.find()) { //store currline's findings into var
                        curr_game_achgotten = matcher_ach.group(1);
                        curr_game_achmax = matcher_ach.group(2);
                        //if dont find ach line for game, both should be empty "".
                        System.out.println("    ach unlocked: "+ curr_game_achgotten + " of " + curr_game_achmax);
                    }

                } catch (Exception e) {
                    System.out.println("unhandled exception "+e.toString()+" on source line, skipping it: " + currLine);
                }
            }
            //if end of input file, output curr game, as if a new fav line was found.
            //Output previous game.
            writer.append(curr_game_name+";");
            writer.append(curr_game_platform+";");
            writer.append(curr_game_hours.toString()+";");
            writer.append(curr_game_ratingstars.toString()+";");
            writer.append(curr_game_fav+";");
            writer.append(curr_game_achgotten+";");
            writer.append(curr_game_achmax+";");
            writer.append(curr_game_rank+";");
            writer.append('\n');

            System.out.println("writing to output file...");
            writer.flush();
            writer.close();
            System.out.println("done.");
        }
        catch(IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) { try { br.close(); } catch(Throwable t) { /* ensure close happens */ } }
            if (r != null) { try { r.close(); } catch(Throwable t) { /* ensure close happens */ } }
            if (ins != null) { try { ins.close(); } catch(Throwable t) { /* ensure close happens */ } }
        }



    }

}
