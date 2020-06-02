package cloud.foundry.cli.operations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cloud.foundry.cli.crosscutting.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.beans.ApplicationManifestBean;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.Applications;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.GetApplicationManifestRequest;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.cloudfoundry.operations.applications.Route;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Test for {@link ApplicationOperations}
 */
public class ApplicationOperationsTest {

    @Test
    public void testGetApplicationsWithEmptyMockData() {
        // prepare mock CF API client with an empty applications list
        DefaultCloudFoundryOperations cfMock = createMockCloudFoundryOperations(Collections.emptyList(),
                Collections.emptyList());

        // forge YAML document
        ApplicationOperations applicationOperations = new ApplicationOperations(cfMock);
        Map<String, ApplicationBean> apps = applicationOperations.getAll().block();

        // check if it's really empty
        assertTrue(apps.isEmpty());
    }

    @Test
    public void testGetApplicationsWithMockData() {
        // create a mock CF API client
        // first, we need to prepare some ApplicationSummary and ApplicationManifest
        // (we're fine with one of both for now)
        // those are then used to create a CF mock API object, which will be able to return those then the right way
        ApplicationManifest appManifest = createMockApplicationManifest();
        ApplicationSummary summary = createMockApplicationSummary(appManifest);

        // now, let's create the mock object from that list
        DefaultCloudFoundryOperations cfMock = createMockCloudFoundryOperations(List.of(summary), List.of(appManifest));

        // now, we can generate a YAML doc for our ApplicationSummary
        ApplicationOperations applicationOperations = new ApplicationOperations(cfMock);
        Map<String, ApplicationBean> apps = applicationOperations.getAll().block();

        // ... and make sure it contains exactly what we'd expect
        assertThat(apps.size(), is(1));
        assertTrue(apps.containsKey("notyetrandomname"));
        ApplicationBean appBean = apps.get("notyetrandomname");
        assertThat(appBean.getPath(), is("/test/uri"));
        assertThat(appBean.getManifest().getBuildpack(), is("test_buildpack"));
        assertThat(appBean.getManifest().getCommand(), is("test command"));
        assertThat(appBean.getManifest().getDisk(), is(1234));
        assertThat(appBean.getManifest().getEnvironmentVariables().size(), is(1));
        assertThat(appBean.getManifest().getEnvironmentVariables().get("key"), is("value"));
        assertThat(appBean.getManifest().getHealthCheckHttpEndpoint(), is("http://healthcheck.local"));
        assertThat(appBean.getManifest().getHealthCheckType(), is(ApplicationHealthCheck.HTTP));
        assertThat(appBean.getManifest().getInstances(), is(42));
        assertThat(appBean.getManifest().getMemory(), is(Integer.MAX_VALUE));
        assertThat(appBean.getManifest().getNoRoute(), is(false));
        assertThat(appBean.getManifest().getRandomRoute(), is(true));
        assertThat(appBean.getManifest().getRoutes().size(), is(2));
        assertThat(appBean.getManifest().getRoutes(), contains("route1", "route2"));
        assertThat(appBean.getManifest().getServices().size(), is(1));
        assertThat(appBean.getManifest().getServices(), contains("serviceomega"));
        assertThat(appBean.getManifest().getStack(), is("nope"));
        assertThat(appBean.getManifest().getTimeout(), is(987654321));
    }

    @Test
    public void testCreateApplicationsPushesAppManifestSucceeds() throws CreationException {
        //given
        ApplicationManifest appManifest = createMockApplicationManifest();
        DefaultCloudFoundryOperations cfoMock = Mockito.mock(DefaultCloudFoundryOperations.class);
        Applications applicationsMock = Mockito.mock(Applications.class);
        Mono<Void> monoMock = Mockito.mock(Mono.class);

        ApplicationOperations applicationOperations = new ApplicationOperations(cfoMock);

        when(cfoMock.applications()).thenReturn(applicationsMock);
        when(cfoMock.applications().get(any(GetApplicationRequest.class)))
                .thenThrow(new IllegalArgumentException());
        when(applicationsMock.pushManifest(any(PushApplicationManifestRequest.class)))
                .thenReturn(monoMock);
        when(monoMock.onErrorContinue( any(Predicate.class), any())).thenReturn(monoMock);
        when(monoMock.block()).thenReturn(null);

        ApplicationBean applicationsBean = new ApplicationBean(appManifest);

        //when
        applicationOperations.create("appName", applicationsBean, false);

        //then
        verify(applicationsMock, times(1)).pushManifest(any(PushApplicationManifestRequest.class));
        verify(monoMock, times(2)).onErrorContinue( any(Predicate.class), any());
        verify(monoMock, times(1)).block();
    }

    @Test
    public void testCreateApplicationsOnMissingDockerPasswordThrowsCreationException() {
        //given
        DefaultCloudFoundryOperations cfoMock = Mockito.mock(DefaultCloudFoundryOperations.class);
        Applications applicationsMock = Mockito.mock(Applications.class);

        ApplicationOperations applicationOperations = new ApplicationOperations(cfoMock);

        when(cfoMock.applications()).thenReturn(applicationsMock);
        when(cfoMock.applications().get(any(GetApplicationRequest.class)))
                .thenThrow(new IllegalArgumentException());
        when(applicationsMock.delete(Mockito.mock(DeleteApplicationRequest.class))).then(Mockito.mock(Answer.class));

        ApplicationBean applicationsBean = new ApplicationBean();
        ApplicationManifestBean applicationManifestBean = new ApplicationManifestBean();
        applicationManifestBean.setDockerImage("some/image");
        applicationManifestBean.setDockerUsername("username");

        applicationsBean.setManifest(applicationManifestBean);

        //when
        CreationException exception = assertThrows(CreationException.class,
                () -> applicationOperations.create("appName", applicationsBean, false));
        assertThat(exception.getMessage(), containsString("Docker password not set"));
    }

    @Test
    public void testCreateApplicationsOnFatalCreationErrorThrowsCreationException() {
        //given
        ApplicationManifest appManifest = createMockApplicationManifest();
        DefaultCloudFoundryOperations cfoMock = Mockito.mock(DefaultCloudFoundryOperations.class);
        Applications applicationsMock = Mockito.mock(Applications.class);

        ApplicationOperations applicationOperations = new ApplicationOperations(cfoMock);

        when(cfoMock.applications()).thenReturn(applicationsMock);
        when(cfoMock.applications().get(any(GetApplicationRequest.class)))
                .thenThrow(new IllegalArgumentException());
        when(applicationsMock.pushManifest(any(PushApplicationManifestRequest.class)))
                .thenThrow(new IllegalArgumentException());
        when(applicationsMock.delete(Mockito.mock(DeleteApplicationRequest.class))).then(Mockito.mock(Answer.class));
        ApplicationBean applicationsBean = new ApplicationBean(appManifest);

        //when
        assertThrows(CreationException.class, () -> applicationOperations.create("appName", applicationsBean, false));
        verify(applicationsMock, times(1)).pushManifest(any());
        verify(applicationsMock, times(1)).delete(any());
    }

    @Test
    public void testCreateApplicationsFailsWhenAlreadyExisting() {
        //given
        ApplicationManifest mockAppManifest = createMockApplicationManifest();
        DefaultCloudFoundryOperations cfoMock = Mockito.mock(DefaultCloudFoundryOperations.class);

        ApplicationOperations applicationOperations = new ApplicationOperations(cfoMock);
        Applications applicationsMock = Mockito.mock(Applications.class);

        when(cfoMock.applications()).thenReturn(applicationsMock);
        when(cfoMock.applications().get(any(GetApplicationRequest.class)))
                .thenReturn(Mockito.mock(Mono.class));

        ApplicationBean applicationsBean = new ApplicationBean(mockAppManifest);

        //then
        assertThrows(CreationException.class,
                () -> applicationOperations.create("appName", applicationsBean, false));
    }

    @Test
    public void testCreateOnNullNameThrowsNullPointerException() throws CreationException {
        //given
        ApplicationOperations applicationOperations = new ApplicationOperations(
                Mockito.mock(DefaultCloudFoundryOperations.class));

        //then
        assertThrows(NullPointerException.class,
                () -> applicationOperations.create(null, new ApplicationBean(), false));
    }

    @Test
    public void testCreateOnNullPathAndNullDockerImageThrowsIllegalArgumentException() throws CreationException {
        //given
        ApplicationOperations applicationOperations = new ApplicationOperations(
                Mockito.mock(DefaultCloudFoundryOperations.class));

        ApplicationBean applicationBean = new ApplicationBean();
        ApplicationManifestBean manifestBean = new ApplicationManifestBean();
        manifestBean.setDockerImage(null);
        applicationBean.setManifest(manifestBean);

        //when
        assertThrows(IllegalArgumentException.class,
                () -> applicationOperations.create("appName", applicationBean, false));
    }

    @Test
    public void testCreateOnNullPathAndNullManifestThrowsIllegalArgumentException() throws CreationException {
        //given
        ApplicationOperations applicationOperations = new ApplicationOperations(
                Mockito.mock(DefaultCloudFoundryOperations.class));
        ApplicationBean applicationBean = new ApplicationBean();

        //when
        assertThrows(IllegalArgumentException.class,
                () -> applicationOperations.create("appName", applicationBean, false));
    }

    @Test
    public void testCreateOnEmptyNameThrowsIllegalArgumentException() {
        //given
        ApplicationOperations applicationOperations = new ApplicationOperations(
                Mockito.mock(DefaultCloudFoundryOperations.class));

        ApplicationBean applicationBean = new ApplicationBean();
        applicationBean.setPath("some/path");

        //then
        assertThrows(IllegalArgumentException.class,
                () -> applicationOperations.create("", applicationBean, false));
    }

    @Test
    public void testCreateOnNullBeanThrowsNullPointerException() {
        //given
        ApplicationOperations applicationOperations = new ApplicationOperations(
                Mockito.mock(DefaultCloudFoundryOperations.class));

        //when
        assertThrows(NullPointerException.class, () -> applicationOperations.create("appName", null, false));
    }


    /**
     * Creates and configures mock object for CF API client
     * We only have to patch it so far as that it will return our own list of ApplicationSummary instances
     * @param appSummaries List of ApplicationSummary objects that the mock object shall return
     * @return mock {@link DefaultCloudFoundryOperations} object
     */
    private DefaultCloudFoundryOperations createMockCloudFoundryOperations(List<ApplicationSummary> appSummaries,
                                                                           List<ApplicationManifest> manifests) {
        // first, we create the mock objects we want to return later on
        DefaultCloudFoundryOperations cfMock = Mockito.mock(DefaultCloudFoundryOperations.class);
        Applications applicationsMock = Mockito.mock(Applications.class);
        Flux<ApplicationSummary> flux = Flux.fromIterable(appSummaries);

        // then we mock the necessary method calls
        Mockito.when(cfMock.applications()).thenReturn(applicationsMock);
        // now, let's have the same fun for the manifests, which are queried in a different way
        // luckily, we already have the applicationsMock, which we also need to hook on here
        // unfortunately, the method matches a string on some map, so we have to rebuild something similar
        // the following lambda construct does exactly that: search for the right manifest by name in the list we've
        // been passed, and return that if possible (or otherwise throw some exception)
        // TODO: check which exception to throw
        Mockito.when(applicationsMock.getApplicationManifest(any(GetApplicationManifestRequest.class)))
                .thenAnswer((Answer<Mono<ApplicationManifest>>) invocation -> {
                    GetApplicationManifestRequest request = invocation.getArgument(0);

                    // simple linear search; this is not about performance, really
                    for (ApplicationManifest manifest : manifests) {
                        if (manifest.getName().equals(request.getName())) {
                            // we need to return a mono mock object so that
                            return Mono.just(manifest);
                        }
                    }

                    throw new RuntimeException("fixme");
                });
        Mockito.when(applicationsMock.list()).thenReturn(flux);

        return cfMock;
    }

    /**
     * Creates an {@link ApplicationManifest} with partially random data to increase test reliability.
     * @return application manifest containing test data
     */
    // FIXME: randomize some data
    private ApplicationManifest createMockApplicationManifest() {
        Map<String, String> envVars = new HashMap<>();
        envVars.put("key", "value");

        // note: here we have to insert a path, too!
        // another note: routes and hosts cannot both be set, so we settle with hosts
        // yet another note: docker image and buildpack cannot both be set, so we settle with buildpack
        ApplicationManifest manifest = ApplicationManifest.builder()
                .buildpack("test_buildpack")
                .command("test command")
                .disk(1234)
                .environmentVariables(envVars)
                .healthCheckHttpEndpoint("http://healthcheck.local")
                .healthCheckType(ApplicationHealthCheck.HTTP)
                .instances(42)
                .memory(Integer.MAX_VALUE)
                .name("notyetrandomname")
                .noRoute(false)
                .path(Paths.get("/test/uri"))
                .randomRoute(true)
                .routes(Route.builder().route("route1").build(), Route.builder().route("route2").build())
                .services("serviceomega")
                .stack("nope")
                .timeout(987654321)
                .build();

        return manifest;
    }

    /**
     * Creates an {@link ApplicationSummary} from an {@link ApplicationManifest} for testing purposes.
     * @return application summary
     */
    // FIXME: randomize some data
    private ApplicationSummary createMockApplicationSummary(ApplicationManifest manifest) {
        // we basically only need the manifest as we need to keep the names the same
        // however, the summary builder complains if a few more attributes aren't set either, so we have to set more
        // than just the name
        ApplicationSummary summary = ApplicationSummary.builder()
                .name(manifest.getName())
                .diskQuota(100)
                .id("summary_id")
                .instances(manifest.getInstances())
                .memoryLimit(manifest.getMemory())
                .requestedState("SOMESTATE")
                .runningInstances(1)
                .build();
        return summary;
    }

}
