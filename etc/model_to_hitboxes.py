#!/usr/bin/env python

from __future__ import division
import json
import collections
import os.path as path
import sys

INLINES = {
    'from' : ("[","]"),
    'to' : ("[","]"),
}

INLINE_TRIGGERS = dict()
for (k, (start, stop)) in INLINES.items():
    INLINE_TRIGGERS['"%s": %s' % (k, start)] = stop


def load_json(path):
    with open(path, "r") as input:
        return json.load(input, object_pairs_hook = collections.OrderedDict, parse_float = lambda x: round(float(x) * 10) / 10)

def transform(model):
    result = list()
    for el in model['elements']:
        name = el['name']
        if name.startswith("ignore"): continue
        result.append({
            'name' : el['name'],
            'from' : el['from'],
            'to' : el['to']
        })

    print "Generated %d hitboxes" % len(result)
    return result;


def dump_json(path, hitboxes):
    with open(path, "wb") as out:
        r = json.dumps(hitboxes, indent=4)

        o = list()
        inline_end = None
        buf = ""
        for line in r.splitlines():
            ls = line.strip()
            if inline_end:
                buf += " "
                buf += ls
                if ls.startswith(inline_end):
                    o.append(buf)
                    inline_end = None
            else:
                if ls in INLINE_TRIGGERS:
                    buf = line.rstrip()
                    inline_end = INLINE_TRIGGERS[ls]
                else:
                    o.append(line)


        out.write("\n".join(o))

def process_file(p):
    print "Processing file: " + path.abspath(p)
    model = load_json(p)
    hitboxes = transform(model)

    (dir, filename) = path.split(p)
    outfile = path.join(dir, "hitboxes_" + filename)
    print "Saving to file: " + path.abspath(outfile)
    dump_json(outfile, hitboxes)

if __name__ == "__main__":
    process_file(sys.argv[1])


