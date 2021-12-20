def parsetxt(filename):
    file = open(filename, "r")
    sumts = 0
    sumtj = 0
    counter = 0

    for line in file:
        line = line.split(",")
        sumts += int(line[0])
        sumtj += int(line[1])
        counter += 1

    print("ts: {0:.3f}, tj: {1:.3f}".format(
        (sumts/counter) / 10**6., (sumtj/counter) / 10**6))


filename = "jmeter-log.txt"
parsetxt(filename)