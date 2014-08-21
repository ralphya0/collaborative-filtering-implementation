collaborative-filtering-implementation
======================================
 Implementation of Collaborative Filtering Algorithm.

 This program is used to generate recommendation goods lists for users based on their shopping transaction log.
 Collaborative filtering is the underlying algorithm of this program which works with Jacarrd Matrix and Pearson
 Matrix separately, after computation, bring out two recommendation lists respectively.

 Required input file should looks like this :
 

 9842500,16199,0,5月6日
 
 4356600,13299,3,5月8日
 
 ...

 Each row in this file is an entry of transaction log. The first column stands for user id, the second one means
 brand id, the third one is action code(0 means click event, 1 means cart event,2 means favorite event and 3
 means buy event) and the last column is time stamp of this entry.

 This program do some pre-process(combine duplicate entries and then sort entries) against original input file
 before computation actually starts. After that, program will build score matrix according to the sorted transaction
 log. With the score matrix we can now generate Jacarrd correlation matrix and Pearson correlation matrix which then
 are used to compute correlation coefficients separately, the correlation coefficients indicate the probability of
 user finally buy a particular brand in future.

 USAGE:
 
 First, you need to compile this source code;
 
 then you can run this program in command line like this :
 
 java CollaborativeFiltering input_file_path output_dir click_threshold neighbor_threshold length_limit

 About the arguments above:
 
 "input_file_path" is the path of input file;
 
 "output_dir" points to the directory in which you want to store results;
 
 "click_threshold" gives the threshold used to score click event from user to the same brand, once click times beyond
 this threshold then this brand will be assigned a high score during the process of building score matrix;
 
 "neighbor_threshold" gives the filter factor which is been used to value if the relativity is strong enough
 between two users.
 
 "length_limit" exposes limitation on the number of recommendation goods for each user . This argument is optional.

 @author : ralphya0

