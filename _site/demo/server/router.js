function route(handle, pathname) {

    console.log("About to route a request for " + pathname);
    debugger;
    if (typeof handle[pathname] !== 'function') {
        console.log("No request handler found for " + pathname);
    } else {
        handle[pathname]();
    }
}

var requestHandlers = require("./requestHandlers");

var handle = {}
handle["/"] = requestHandlers.start;
handle["/start"] = requestHandlers.start;
/** @namespace requestHandlers.upload */
handle["/upload"] = requestHandlers.upload;
route(handle["/"])

exports.route = route;