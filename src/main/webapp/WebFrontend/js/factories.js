'use strict';

var pocketdocFactories = angular.module('pocketdocFactories', []),
    root = "http://pockedoc.herokuapp.com";

/* Factories */
//pocketdocFactories.factory("nextQuestionFactory", function ($resource) {
//    var baseUrl = root + "/nextQuestion/user/:Id";
//    return $resource(
//        baseUrl, {Id: '@Id'},{
//            'get': { method: 'GET'}
//        }
//    );
//});
//
//pocketdocFactories.factory("runFactory", function($http, $resource){
//    var factory = {};
//
//    var baseUrl = root+"/run/user/:Id";
//
//    return $resource(
//        baseUrl, { Id: '@Id' },{
//            'sendAnswer': { method:'PUT' },
//            'resetRun': {method: 'DELETE'},
//            'getDiagnosis': {method: 'GET'}
//        }
//    );
//});

pocketdocFactories.factory("runFactory", function($http, $resource){
    var factory = {};

    var baseUrl = "../run/user/:Id";

    return $resource(
        baseUrl, { Id: '@Id' },{
            'sendAnswer': { method:'PUT' },
            'resetRun': {method: 'DELETE'},
            'getDiagnosis': {method: 'GET'}
        }
    );
});

pocketdocFactories.factory("nextQuestionFactory", function ($resource) {
    var baseUrl = "../nextQuestion/user/:Id";
    return $resource(
        baseUrl, {},{
            'getNextQuestion': { method: 'POST'}
        }
    );
});

pocketdocFactories.factory("loginFactory", function($http, $resource){

    var baseUrl = "../login";

    return $resource(baseUrl, {},{
        'login': {method: 'PUT'}
    });

});

pocketdocFactories.factory("logoutFactory", function($http, $resource){

    var baseUrl = "../logout";

    return $resource(baseUrl, {},{
        'logout': {method: 'POST'}
    });

});

pocketdocFactories.factory("userFactory", function($http, $resource){
    var baseUrl = "../user";

    return $resource(
        baseUrl, {},{
            'isInUse': {method: 'POST'},
            'createUser': {method: 'POST'},
            'updateUser': {method: 'PUT'},
            'getUser': {method: 'GET'},
            'deleteUser': {method: 'DELETE'}
        }
    );
});

pocketdocFactories.factory('_', function() {
  return window._; //Underscore must already be loaded on the page
});