/* LD_PRELOAD shim for Minecraft 26.3 under Xvfb.
 * SDL3 asks for an sRGB GLX framebuffer. Xvfb's llvmpipe visuals are sRGB=N,
 * so glXChooseFBConfig fails and the client never opens a window.
 * This removes that attribute and, if SDL checks the chosen config, reports
 * the capability anyway. Load it only for 26.3: the same preload trips
 * glXGetProcAddress on the GLFW-based 26.2 client.
 */
#define _GNU_SOURCE
#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define GLX_FRAMEBUFFER_SRGB_CAPABLE_ARB 0x20B2
#define GLX_NONE 0

static int *strip_srgb(const int *attribs) {
    int count;
    int *copy;
    int write;
    int read;
    if (attribs == NULL) {
        return NULL;
    }
    count = 0;
    while (attribs[count] != GLX_NONE) {
        count++;
        if (count > 256) {
            return NULL;
        }
    }
    copy = malloc((size_t)(count + 1) * sizeof(int));
    if (copy == NULL) {
        return NULL;
    }
    write = 0;
    read = 0;
    while (read < count) {
        if (attribs[read] == GLX_FRAMEBUFFER_SRGB_CAPABLE_ARB) {
            read += 2;
            continue;
        }
        copy[write++] = attribs[read++];
    }
    copy[write] = GLX_NONE;
    return copy;
}

void *glXChooseFBConfig(void *dpy, int screen, const int *attrib_list, int *nelements) {
    static void *(*real_fn)(void *, int, const int *, int *) = NULL;
    int *stripped;
    void *chosen;
    if (real_fn == NULL) {
        real_fn = dlsym(RTLD_NEXT, "glXChooseFBConfig");
    }
    stripped = strip_srgb(attrib_list);
    chosen = real_fn(dpy, screen, stripped != NULL ? stripped : attrib_list, nelements);
    free(stripped);
    return chosen;
}

int glXGetFBConfigAttrib(void *dpy, void *config, int attribute, int *value) {
    static int (*real_fn)(void *, void *, int, int *) = NULL;
    int result;
    if (real_fn == NULL) {
        real_fn = dlsym(RTLD_NEXT, "glXGetFBConfigAttrib");
    }
    if (attribute == GLX_FRAMEBUFFER_SRGB_CAPABLE_ARB && value != NULL) {
        *value = 1;
        return 0;
    }
    result = real_fn(dpy, config, attribute, value);
    return result;
}

static void *resolve(const unsigned char *name) {
    static void *(*real_get)(const unsigned char *) = NULL;
    if (real_get == NULL) {
        real_get = dlsym(RTLD_NEXT, "glXGetProcAddress");
        if (real_get == NULL) {
            real_get = dlsym(RTLD_NEXT, "glXGetProcAddressARB");
        }
    }
    if (name != NULL && strcmp((const char *)name, "glXChooseFBConfig") == 0) {
        return (void *)glXChooseFBConfig;
    }
    if (name != NULL && strcmp((const char *)name, "glXGetFBConfigAttrib") == 0) {
        return (void *)glXGetFBConfigAttrib;
    }
    if (real_get == NULL) {
        return NULL;
    }
    return real_get(name);
}

void *glXGetProcAddress(const unsigned char *name) {
    return resolve(name);
}

void *glXGetProcAddressARB(const unsigned char *name) {
    return resolve(name);
}
