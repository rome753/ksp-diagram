import os
import sys
import json
import webbrowser

openBrowser = True

def visit(path):
    if os.path.isdir(path):
        if os.path.basename(path) == 'build':
            return None
        arr = os.listdir(path)
        ret = []
        for name in arr:
            p = os.path.join(path, name)
            r = visit(p)
            if r is not None:
                ret.append(r)
        if not ret:
            return None
        if len(ret) == 1 and isinstance(ret[0], dict):
            return {
                'name': os.path.basename(path) + '/' + ret[0]['name'],
                'list': ret[0]['list']
            }
        return {
            'name': os.path.basename(path),
            'list': ret
        }
    else:
        if path.endswith('.kt') or path.endswith('.java'):
            print('visit: ' + path)
            return os.path.basename(path)
        return None


if __name__ == '__main__':
    argc = len(sys.argv)
    print('argv0 ' + sys.argv[0])
    pyPath = os.path.dirname(os.path.normpath(os.path.abspath(sys.argv[0]))) # ksp
    genPath = os.path.join(pyPath, 'build', 'generated')
    projPath = os.path.dirname(pyPath)
    if argc > 1:
        print('argv1 ' + sys.argv[1])
        openBrowser = True
        projPath = sys.argv[1]
    
    print('projPath ' + projPath)

    os.chdir(projPath)
    tree = visit(os.getcwd())
    if os.path.exists(genPath) == False:
        os.mkdir(genPath)
    data = os.path.join(genPath, 'dir.json')
    f = open(data, 'w')
    f.write(json.dumps(tree))
    f.close()


    appModuleName = 'app'
    debugName = 'debug'
    resPath = os.path.join(projPath, appModuleName, 'build', 'generated', 'ksp', debugName, 'resources')
    print('resPath ' + resPath)
    f1 = os.path.join(resPath, 'nodes.json')
    f2 = os.path.join(resPath, 'edges.json')

    if os.path.exists(f1) and os.path.exists(f2):

        f1_new = os.path.join(genPath, os.path.basename(f1))
        f2_new = os.path.join(genPath, os.path.basename(f2))

        if os.path.exists(f1_new):
            os.remove(f1_new)
        if os.path.exists(f2_new):
            os.remove(f2_new)

        os.rename(f1, f1_new)
        os.rename(f2, f2_new)

        print('move ' + f1 + ' to ' + f1_new)
        print('move ' + f2 + ' to ' + f2_new)


    if openBrowser:
        os.chdir(pyPath)
        print(os.getcwd())
        webbrowser.open('http://localhost:8080/diagram.html')
        os.system('python -m http.server 8080')