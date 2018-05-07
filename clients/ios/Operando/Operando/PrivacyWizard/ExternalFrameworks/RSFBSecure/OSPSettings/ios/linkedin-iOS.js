
  (function(privacySettingsJsonString) {
   
   var kMessageTypeKey = "messageType";
   var kLogMessageTypeContentKey = "logContent";
   var kLogMessageType = "log";
   
   var kStatusMessageMessageType = "statusMessageType";
   var kStatusMessageContentKey = "statusMessageContent";
   
   var webkitSendMessage = function(message) {
   alert(message);
   };
   
   window.console = {};
   window.console.log = function(logMessage) {
   var webkitMessage = {};
   webkitMessage[kMessageTypeKey] = kLogMessageType;
   webkitMessage[kLogMessageTypeContentKey] = logMessage;
   
   webkitSendMessage(JSON.stringify(webkitMessage));
   };
   
   var sendStatusMessage = function(settingName) {
   var webkitMessage = {};
   webkitMessage[kMessageTypeKey] = kStatusMessageMessageType;
   webkitMessage[kStatusMessageContentKey] = settingName;
   webkitSendMessage(JSON.stringify(webkitMessage));
   };
   
   var privacySettings = JSON.parse(privacySettingsJsonString);
   var csrfToken = {};
   
   function preparePostRequest(settings, headers, resolve, reject) {
   
    console.log("AICI2")
   
   var data = {};
   
   if (!data.hasOwnProperty("topLevelNodeId")) {
   data["topLevelNodeId"] = "root";
   }
   
   console.log("AICI")
   
   for (var prop in settings.data) {
   data[prop] = settings.data[prop];
   }
   

   console.log("AICI3")
   
   for (var param in settings.params) {
   if (settings.params[param].type && settings.params[param].type === "dynamic") {
   if (headers[param]) {
   settings.url = settings.url.replace("{" + settings.params[param].placeholder + "}", headers[param]);
   }
   }
   }
   console.log("URL:"+settings.page);
   if (settings.type == "application/json") {
   $.ajax({
          type: "POST",
          url: settings.url,
          data: JSON.stringify(data),
          contentType: 'application/json; charset=utf-8',
          dataType: "json",
          beforeSend: function(request) {
          if (settings.headers) {
          for (var i = 0; i < settings.headers.length; i++) {
          var header = settings.headers[i];
          request.setRequestHeader(header.name, header.value);
          }
          }
          
          request.setRequestHeader("accept", "application/json, text/javascript, */*; q=0.01");
          request.setRequestHeader("accept-language", "en-US,en;q=0.8");
          request.setRequestHeader("X-Alt-Referer", settings.page);
          
          },
          success: function(result) {
          //                                    resolve(result);
          },
          statusCode: {
          500: function() {
          console.log("Sunt in 500");
          }
          },
          error: function(a, b, c) {
          //                                    reject(b);
          console.log(a + b + c)
          },
          complete: function(request, status) {
          console.log("statusBUN: " + status);
          
          resolve();
          }
          
          });
   } else {
   
   var formData = new FormData();
   for (var p in headers) {
   formData.append(p, headers[p]);
   }
   
   for (var prop in data) {
   formData.append(prop, data[prop]);
   }
   $.ajax({
          type: "POST",
          url: settings.url,
          data: formData,
          contentType: false,
          processData: false,
          
          
          beforeSend: function(request) {
          
          request.setRequestHeader("accept", "*/*");
          request.setRequestHeader("accept-language", "en-US,en;q=0.8");
          request.setRequestHeader("X-Alt-Referer", settings.page);
          },
          success: function(result) {
          setTimeout(function() {
                     resolve(result);
                     }, 0);
          },
          statusCode: {
          500: function() {
          console.log("Sunt in 500");
          },
          400: function(a, b) {
          console.log(a.responseText);
          }
          },
          error: function(a, b, c) {
          reject(c);
          },
          complete: function(request, status) {
          //                     console.log(request.responseText);
          }
          
          });
   }
   
   }
   
   
   function postToLinkedIn(settings, item, total) {

    console.log("postToLinkedIn")
   
   
   return new Promise(function(resolve, reject) {
                      
                      if (settings.page) {

                        console.log("CSRF TOKEN = " + JSON.stringify(csrfToken))
                      
                      if (Object.keys(csrfToken).length>0) {
                                          console.log("works4");
                      //TO DO: count no of links
                      preparePostRequest(settings, csrfToken, resolve, reject);
                      } else {
                      console.log("works5");
                      doGET(settings.page, function(response) {


                            console.log(response);

                            console.log("works6");
                            csrfToken = extractHeaders(response);
                            preparePostRequest(settings, csrfToken, resolve, reject);
                            
                            });
                      }
                      
                      }
                      });
   }
   
   function secureAccount(callback) {
   var total = privacySettings.length;
   var sequence = Promise.resolve();
   privacySettings.forEach(function(settings, index) {
                           
                           sequence = sequence.then(function() {
                                                    
                                                    if (privacySettings.length == index+1){
                                                    sendStatusMessage("Done")
                                                    }
                                                    else {
                                                    var text = "DONE PROGRESS " + "item=" + index + "total=" + total
                                                    sendStatusMessage(text);
                                                    }
                                                    
                                                    
                                                    
                                                    return postToLinkedIn(settings, index, total);
                                                    
                                                    }).then(function(result) {}).catch(function(err) {
                                                                                       console.log(err);
                                                                                       });
                           });
   
   
   sequence = sequence.then(function(result) {
                            
                            callback();
                            });
   
   
   }
   
   function doGET(page, callback) {

    console.log("doGET PAGE = " + page)

   $.ajax({
          url: page,
          method:"GET",
          success: callback,
          dataType: 'html',
          error: function(a, b, c) {
            console.log( "EROARE" + a + b + c)
          }
          });
   }
   
   
   function extractHeaders(content) {
   var csrfToken = /<meta name="lnkd-track-error" content\=\"\/lite\/ua\/error\?csrfToken=(ajax%3A[0-9]*)\">/;
   var data = {};
   var match;
   
   if ((match = csrfToken.exec(content)) !== null) {
   if (match.index === csrfToken.lastIndex) {
   csrfToken.lastIndex++;
   }
   }
   data['csrfToken'] = decodeURIComponent(match[1]);
   // console.log(data['csrfToken']);
   return data;
   
   }
   secureAccount(function() {
                 });
   })(RS_PARAM_PLACEHOLDER)