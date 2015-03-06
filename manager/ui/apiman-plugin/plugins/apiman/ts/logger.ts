/// <reference path="../../includes.ts"/>
module ApimanLogger {

    export var _module = angular.module("ApimanLogger", []);

    var _formatMessage = function(theArgs) {
        var now = new Date();
        var msg = theArgs[0];
        if (theArgs.length > 1) {
            for (var i = 1; i < theArgs.length; i++) {
                msg = msg.replace('{'+(i-1)+'}', theArgs[i]);
            }
        }
        return 'apiman [' + now.toLocaleTimeString() + ']>>  ' + msg;
    };
    
    export var Logger = _module.factory('Logger', 
        function() {
            return {
                log: function() {
                    console.info(_formatMessage(arguments));
                },
                debug: function() {
                    console.warn(_formatMessage(arguments));
                },
                error: function() {
                    console.error(_formatMessage(arguments));
                }
            };
        });

}