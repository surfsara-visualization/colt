#!/usr/bin/env python
#
# Screw make and scons, we'll do it like this

import os, sys, subprocess, time

CLASSES = ['ColtBuild', 'SaraTouchClient', 'ByteBufferView', 'ConnectionHandler',
        'TouchPoint', 'TouchEventHandler', 'CalibrationPanel',
        'TuioTouchHistory', 'Timestamp', 'point2']

source_files = [x+'.java' for x in CLASSES]
class_files = [x+'.class' for x in CLASSES]

# Get revision and store to file

p = subprocess.Popen(['git', 'rev-parse', 'HEAD'], stdout=subprocess.PIPE)
stdout, stderr = p.communicate()
git_id = stdout.strip()

p = subprocess.Popen(['git', 'diff-index', '--name-only', 'HEAD'], stdout=subprocess.PIPE)
stdout, stderr = p.communicate()
if stdout != '':
    git_id += '+'

timestamp = time.strftime('%Y%m%d-%H%M%S', time.localtime(time.time()))

f = open('ColtBuild.java', 'wt')
f.write("""
public class ColtBuild
{
    public final static String revision = "%s";
    public final static String build_date = "%s";
}
""" % (git_id, time.asctime(time.localtime(time.time()))))
f.close()

# Compile to class files

os.system('javac %s' % ' '.join(source_files))

if len(sys.argv) > 1:
    # Make .jar
    jarfile = 'colt-%s-%s.jar' % (git_id, timestamp)
    os.system('jar cfm %s Manifest.txt %s' % (jarfile, ' '.join(class_files)))
    if sys.platform == 'linux2':
        # Make symlink named colt.jar to just created jar
        try:
            os.unlink('colt.jar')
        except OSError:
            pass
        os.symlink(jarfile, 'colt.jar')
else:
    print 'Not making jar (not requested)!'
