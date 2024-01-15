const hideFiles = {}
const hideEdges = {}
var nodeArr = []
var edgeArr = []
var nodesView = undefined
var edgesView = undefined

main();

function main() {
    Promise.all([
        readTextFile('build/generated/dir.json'),
        readTextFile('build/generated/edges.json'),
        readTextFile('build/generated/nodes.json')
    ]).then(() => {
        console.log('Both files have been successfully read.');
        createDiagram()
    }).catch((error) => {
        console.error('An error occurred:', error);
    });
}

function readTextFile(path) {
    var rawFile = new XMLHttpRequest();
    rawFile.open("GET", path, false);
    rawFile.onreadystatechange = function ()
    {
        if(rawFile.readyState === 4)
        {
            if(rawFile.status === 200 || rawFile.status == 0)
            {
                var allText = rawFile.responseText;
                handleJsonStr(path, allText);
            }
        }
    }
    rawFile.send(null);
}

function handleJsonStr(path, str) {
    var json = JSON.parse(str)
    if (path.endsWith('nodes.json')) {
        nodeArr = json
    } else if (path.endsWith('edges.json')) {
        edgeArr = json
    } else if (path.endsWith('dir.json')) {
        handleTreeJson(json)
    }
}

function handleTreeJson(obj) {
    var tree = document.getElementById("mytree");
    var html = generateTree(obj)
    tree.innerHTML = '<ul>' + html + '</ul>'

    for (var ele of tree.querySelectorAll('div')) {
        // console.log(li.getElementsByClassName('li'))
        ele.addEventListener('click', function(e) {
            var div = e.target   
            var hide = div.style.opacity != 0.3 
            hideDivOrNot(div, hide) 
            nodesView.refresh()
        });
    }
}

function hideDivOrNot(div, hide) {
    if (div.id == 'file') {
        hideFiles[div.innerHTML] = hide
        div.style.opacity = hide ? 0.3 : 1
    } else if (div.id == 'dir') {
        hideListOrNot(div, hide)
    }
}

function hideListOrNot(div, hide) {
    div.style.opacity = hide ? 0.3 : 1
    
    var ul = div.nextElementSibling
    var child = ul.firstElementChild
    while (child != ul.lastElementChild) {
        hideDivOrNot(child.firstElementChild, hide)
        child = child.nextElementSibling
    }
    if (child != undefined) {
        hideDivOrNot(child.firstElementChild, hide)
    }        
}

function generateTree(obj) {
    if (typeof obj == 'string') {
        return `<div id='file'>${obj}</div>`
    } else {
        var str = `<div id='dir'>${obj['name']}</div><ul>`
        for (var o of obj['list']) {
            str += '<li>' + generateTree(o) + '</li>'
        }
        return str + '</ul>'
    }
}

function initEdgeFilterElement() {
    var edgeTypes = ['generalization','realization','association','dependency']
    var div = document.getElementById("mycheck");
    var html = ''
    for (var name of edgeTypes) {
        html += 
        `<div>
            <label>
            <input type="checkbox" name="edgesFilter" value="${name}" checked />
            ${name}
            </label>
        </div>`
    }
    div.innerHTML = html


    const edgeFilters = document.getElementsByName("edgesFilter");
    edgeFilters.forEach((filter) =>
        filter.addEventListener("change", (e) => {
            const { value, checked } = e.target;
            console.log(value)
            console.log(checked)
            hideEdges[value] = !checked
            edgesView.refresh();
        })
    );
}

function createDiagram() {
    for (var node of nodeArr) {
        setNode(node)
    }
    for (var edge of edgeArr) {
        setEdge(edge)
    }

    let nodes = new vis.DataSet(nodeArr)
    let edges = new vis.DataSet(edgeArr)

    initEdgeFilterElement()

    const nodesFilter = (node) => {
        if (hideFiles[node.file] == true) {
            return false
        }
        return true;
    };

    const edgesFilter = (edge) => {
        if (hideEdges[edge.kind] == true) {
            return false
        }
        return true;
    };

    nodesView = new vis.DataView(nodes, { filter: nodesFilter });
    edgesView = new vis.DataView(edges, { filter: edgesFilter });

    // create a network
    var container = document.getElementById("mynetwork");
    var data = {
    nodes: nodesView,
    edges: edgesView,
    };
    var options = {
        physics: createPhysicsConfig(),
        
        // layout: {
        //     hierarchical: {
        //       direction: 'Up-Down',
        //     },
        // },
    };
    var network = new vis.Network(container, data, options);
    // network.stopSimulation()
    // network.on("dragStart", function (params) {
    //     // There's no point in displaying this event on screen, it gets immediately overwritten
    //     params.event = "[original event]";

    //     network.startSimulation()
    // });

    // network.on("dragEnd", function (params) {
    //     params.event = "[original event]";
 
    //     network.stopSimulation()
    // });
}

function setNode(node) {
    var shape = 'box'
    var dashes = false
    switch(node.kind) {
        case 'class':
            break
        case 'struct':
            shape = 'ellipse'
            break
        case 'protocol':
            dashes = true
            break
        case 'enum':
            shape = 'dot'
            break
    }
    Object.assign(node, {
        // label: label,
        title: node.label,
        shape: shape,
        shapeProperties: dashes ? { borderDashes: [5, 5] } : {},
        font: { align: 'left' },
        physics: true,
        // color: { background: "transparent"},
    })
}

function setEdge(edge) {
    var arrowType = 'triangle'
    var scaleFactor = 2
    var dashes = false
    switch(edge.kind) {
        case 'generalization':
            break
        case 'realization':
            dashes = true
            break
        case 'association':
            arrowType = 'vee'
            scaleFactor = 1
            break
        case 'dependency':
            arrowType = 'vee'
            scaleFactor = 1
            dashes = true
            break
    }
    Object.assign(edge, {
        arrows: {
            to: {
                enabled: true,
                type: arrowType,
                scaleFactor: scaleFactor,
            },
        },
        dashes: dashes,
        smooth: { 
            enabled: true,
            type: 'discrete',
        },
        // physics: false,
    })
}

function createPhysicsConfig() {
    return  {
        enabled: false,
        barnesHut: {
          theta: 0.5,
          gravitationalConstant: -2000,
          centralGravity: 0.3,
          springLength: 95,
          springConstant: 0.04,
          damping: 0.09,
          avoidOverlap: 0
        },
        forceAtlas2Based: {
          theta: 0.5,
          gravitationalConstant: -50,
          centralGravity: 0.01,
          springConstant: 0.08,
          springLength: 100,
          damping: 0.4,
          avoidOverlap: 0
        },
        repulsion: {
          centralGravity: 0.2,
          springLength: 200,
          springConstant: 0.05,
          nodeDistance: 100,
          damping: 0.09
        },
        hierarchicalRepulsion: {
          centralGravity: 0.0,
          springLength: 100,
          springConstant: 0.01,
          nodeDistance: 120,
          damping: 0.09,
          avoidOverlap: 0
        },
        maxVelocity: 50,
        minVelocity: 0.1,
        solver: 'barnesHut',
        stabilization: {
          enabled: true,
          iterations: 1000,
          updateInterval: 100,
          onlyDynamicEdges: false,
          fit: true
        },
        timestep: 0.5,
        adaptiveTimestep: true,
        wind: { x: 0, y: 0 }
    }
}