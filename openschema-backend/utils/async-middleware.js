//Used to catch errors in async methods so that the middleware chain can continue
exports.asyncMiddleware = fn =>
    (req, res, next) => {
        Promise.resolve(fn(req, res, next))
            .catch(next);
    };