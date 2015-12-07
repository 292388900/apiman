/// <reference path="../apimanPlugin.ts"/>
/// <reference path="../rpc.ts"/>
module Apiman {
    
    export var AppContractsController = _module.controller("Apiman.AppContractsController",
        ['$q', '$scope', '$location', 'PageLifecycle', 'AppEntityLoader', 'OrgSvcs', 'Logger', 'Dialogs', '$routeParams', 'Configuration',
        ($q, $scope, $location, PageLifecycle, AppEntityLoader, OrgSvcs, Logger, Dialogs, $routeParams, Configuration) => {
            var params = $routeParams;
            $scope.organizationId = params.org;
            $scope.tab = 'contracts';
            $scope.version = params.version;
            $scope.showMetrics = Configuration.ui.metrics;
            var pageData = AppEntityLoader.getCommonData($scope, $location);
            pageData = angular.extend(pageData, {
                contracts: $q(function(resolve, reject) {
                    OrgSvcs.query({ organizationId: params.org, entityType: 'applications', entityId: params.app, versionsOrActivity: 'versions', version: params.version, policiesOrActivity: 'contracts' }, function(contracts) {
                        $scope.filteredContracts = contracts;
                        resolve(contracts);
                    }, reject);
                })
            });
            
            function removeContractFromArray(contract, carray) {
                var idx = -1;
                for (var i = 0; i < carray.length; i++) {
                    if (carray[i].contractId == contract.contractId) {
                        idx = i;
                        break;
                    }
                }
                if (idx > -1) {
                    carray.splice(idx, 1);
                }
            };
            
            $scope.filterContracts = function(value) {
                Logger.debug('Called filterContracts!');
                if (!value) {
                    $scope.filteredContracts = $scope.contracts;
                } else {
                    var fc = [];
                    angular.forEach($scope.contracts, function(contract) {
                        if (contract.apiOrganizationName.toLowerCase().indexOf(value.toLowerCase()) > -1 || contract.apiName.toLowerCase().indexOf(value.toLowerCase()) > -1) {
                            fc.push(contract);
                        }
                    });
                    $scope.filteredContracts = fc;
                }
            };
            
            $scope.breakAll = function() {
                Dialogs.confirm('Break All Contracts?', 'Do you really want to break all contracts with all APIs?', function() {
                    OrgSvcs.delete({ organizationId: params.org, entityType: 'applications', entityId: params.app, versionsOrActivity: 'versions', version: params.version, policiesOrActivity: 'contracts' }, function() {
                        $scope.contracts = [];
                        $scope.filteredContracts = [];
                        $scope.version.modifiedOn = Date.now();
                    }, PageLifecycle.handleError);
                });
            };
            
            $scope.break = function(contract) {
                Logger.debug("Called break() with {0}.", contract);
                Dialogs.confirm('Break Contract', 'Do you really want to break this contract?', function() {
                    OrgSvcs.delete({ organizationId: params.org, entityType: 'applications', entityId: params.app, versionsOrActivity: 'versions', version: params.version, policiesOrActivity: 'contracts', policyId: contract.contractId }, function() {
                        removeContractFromArray(contract, $scope.contracts);
                        removeContractFromArray(contract, $scope.filteredContracts);
                        $scope.version.modifiedOn = Date.now();
                    }, PageLifecycle.handleError);
                });
            };
            
            PageLifecycle.loadPage('AppContracts', 'appView', pageData, $scope, function() {
                PageLifecycle.setPageTitle('app-contracts', [ $scope.app.name ]);
            });
        }])

}
