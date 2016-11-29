import random

# 88162 transactions total
n = 88162

line_numbers = []
for i in range(1,n+1):
	line_numbers.append(i);

rand_smpl = [ line_numbers[i] for i in sorted(random.sample(xrange(len(line_numbers)), n/4)) ]
print rand_smpl
