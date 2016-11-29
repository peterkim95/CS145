import random

# 88162 transactions total
n = 88162

line_numbers = []
for i in range(1,n+1):
	line_numbers.append(i);

r1 = [ line_numbers[i] for i in sorted(random.sample(xrange(len(line_numbers)), n/4)) ]
r2 = [ line_numbers[i] for i in sorted(random.sample(xrange(len(line_numbers)), n/4)) ]
r3 = [ line_numbers[i] for i in sorted(random.sample(xrange(len(line_numbers)), n/4)) ]
r4 = [ line_numbers[i] for i in sorted(random.sample(xrange(len(line_numbers)), n/4)) ]

f = open('data/retail.dat', 'r')

f1 = open('data/retail_r4.dat', 'w+')
# f2 = open('data/retail_r2.dat', 'w+')
# f3 = open('data/retail_r3.dat', 'w+')
# f4 = open('data/retail_r4.dat', 'w+')

r = 0
for i,l in enumerate(f):
	if i == r1[r]:
		f1.write(l)
		r = r + 1
	if r >= len(r1):
		break

# r = 0
# for i,l in enumerate(f):
# 	if i == r2[r]:
# 		f2.write(l)
# 		r = r + 1
# 	if r >= len(r2):
# 		break

# r = 0
# for i,l in enumerate(f):
# 	if i == r3[r]:
# 		f3.write(l)
# 		r = r + 1
# 	if r >= len(r3):
# 		break

# r = 0
# for i,l in enumerate(f):
# 	if i == r4[r]:
# 		f4.write(l)
# 		r = r + 1
# 	if r >= len(r4):
# 		break

# print rand_smpl
